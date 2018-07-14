package dk.fitfit.ci.server

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*
import kotlin.concurrent.thread

@SpringBootApplication
class CiServerApplication

fun main(args: Array<String>) {
    runApplication<CiServerApplication>(*args)
}

@RestController
@RequestMapping("/hooks")
class HookController {
    @PostMapping("/github")
    fun github(@RequestBody payload: LinkedHashMap<String, *>) {
        val repository = payload["repository"]
        var name = ""
        if (repository is LinkedHashMap<*, *>) {
            val raw = repository.get("name")
            if (raw is String) {
                name = raw
            }
        }

        var cloneUrl = ""
        if (repository is LinkedHashMap<*, *>) {
            val raw = repository.get("clone_url")
            if (raw is String) {
                cloneUrl = raw
            }
        }

        val headCommit = payload["head_commit"]
        var id = ""
        if (headCommit is LinkedHashMap<*, *>) {
            val raw = headCommit.get("id")
            if (raw is String) {
                id = raw
            }
        }

        thread {
            // TODO: Create build... store timestamp and payload
            // Load context by... repo and branch? and associate with build
            val buildContext = BuildContext(name, cloneUrl, id)
            val processor = ProcessCiRequest()
            processor.build(buildContext)
        }
    }

    @PostMapping("/bitbucket")
    fun bitbucket(@RequestBody payload: Any) {
        val mapper = ObjectMapper()
        val json = mapper.writeValueAsString(payload)
        println(json)
    }
}

// ** Build context...
//source... git... user/pass... ssh
//condition... any valid jsonpath? Could possible point to a branch
//What to build: Service
//Tag...
//artifact storage... user, pass, host
class BuildContext(val name: String, val repository: String, val commitId: String)

@RestController
class BuildController {
    @GetMapping("/builds/{id}}")
    fun build(@PathVariable id: String) {
        TODO("Return build details")
    }

    @GetMapping("/builds")
    fun builds(): String {
        TODO("Return list of builds")
    }
}

// TODO: Should be ProcessService and the build method should take a build object rather context
// TODO: Run all of this in a thread
class ProcessCiRequest {
    fun build(buildContext: BuildContext) {
        // Create volume
        val volumePostfix = UUID.randomUUID().toString()
        val volume = "ci-server-build-$volumePostfix"
        executeCommand("docker volume create --name=$volume")

        // Clone repo
        executeCommand("docker run -t --rm -v $volume:/git alpine/git clone ${buildContext.repository}")

        // Git reset
        executeCommand("docker run -t --rm -v $volume:/git -w /git/${buildContext.name} alpine/git reset --hard ${buildContext.commitId}")

        // Mv .env.dist .env
        executeCommand("docker run -t --rm -v $volume:/src -w /src/${buildContext.name} alpine mv .env.dist .env") // TODO: if not .env and .env.dist or .env.sample or .env.example

        // Run our image on it
        val image = "tons/dc-ci"
        val service = "release"
        val tag = "ci-server-3"
        val registryUser = "tons"
        val registryPass = "skummet"
        // TODO: Convert to docker compose...
        val command = "docker run --rm -t -e DEBUG_PORT=666 -e SERVICE=$service -e TAG=$tag -e REGISTRY_USER=$registryUser -e REGISTRY_PASS=$registryPass -v $volume:/src -w /src/${buildContext.name} -v /var/run/docker.sock:/var/run/docker.sock $image"
        executeCommand(command)

        // Rm volume
        executeCommand("docker volume rm $volume")

        // TODO: Record end of build
        // TODO: Version 2.0
        // register builds... with environment variables
        // http basic auth... single user
        // Run this in a openjdk:alpine image with apk add docker
        // Run a thread? with each build and stream the output somewhere
    }
}

private fun executeCommand(command: String, directory: String = "/tmp") {
    println("Command: $command")
    try {
        val pb = ProcessBuilder(command.split(" "))
        pb.directory(File(directory))
        pb.redirectErrorStream(true)
        val p = pb.start()
        val exit = p.waitFor()
        println("Exit: $exit")
        val reader = BufferedReader(InputStreamReader(p.inputStream))
// TODO: Store build lines in a table with fk build... or somewhere else and concatenate into a blob and store that on the build
        var line = reader.readLine()
        while (line != null) {
            println(line)
            line = reader.readLine()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
