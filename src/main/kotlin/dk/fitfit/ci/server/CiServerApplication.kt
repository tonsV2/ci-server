package dk.fitfit.ci.server

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@SpringBootApplication
class CiServerApplication

fun main(args: Array<String>) {
    runApplication<CiServerApplication>(*args)
}

@RestController
@RequestMapping("/hooks")
class HookController {
    @PostMapping("/github")
    fun github(@RequestBody payload: LinkedHashMap<String, *>): String {
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

        // TODO: Load build context by... repo and branch?
        val buildContext = BuildContext(name, cloneUrl, id)
        val processor = ProcessCiRequest()
        val output = processor.build(buildContext)
        return output
    }

    @PostMapping("/bitbucket")
    fun bitbucket(@RequestBody payload: Any) {
        val mapper = ObjectMapper()
        val json = mapper.writeValueAsString(payload)
        println(json)
    }
}

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

class ProcessCiRequest() {
    fun build(buildContext: BuildContext): String {
        var command = ""
        var output = ""

        command = "rm -rf /tmp/${buildContext.name}"
        output = executeCommand(command, "/tmp")
        println("Output: $output")

        command = "git clone ${buildContext.repository}"
        output = executeCommand(command, "/tmp")
        println("Output: $output")

        command = "git reset --hard ${buildContext.commitId}"
//        output = executeCommand(command, "/tmp/${buildContext.name}")
        println("Output: $output")

        // TODO: if not .env and .env.dist or .env.sample or .env.example
        command = "mv .env.dist .env"
        output = executeCommand(command, "/tmp/${buildContext.name}")
        println("Output: $output")

        // TODO: Fetch image and pretty much the entire line from the build context
        command = "docker run -i -v \"\$PWD\":/src -e ... tons/dc-ci"
//        output = executeCommand(command, "/tmp/${buildContext.name}")
        println("Output: $output")

        command = "docker-compose build release"
        output = executeCommand(command, "/tmp/${buildContext.name}")
        println("Output: $output")

//        command = "docker-compose push release"
//        output = executeCommand(command, "/tmp/${buildContext.name}")
//        println("Output: $output")

//        command = "git reset --hard ${buildContext.commitId}"
//        output = executeCommand(command)
//        println("Output: $output")

//        return executeCommand("docker run -d -t alpine ls")
        return output
        TODO("git clone")
        TODO("docker login")
        TODO("docker compose run test")
        TODO("docker compose build release")
        TODO("docker compose push release")
        // TODO: Version 2.0
        // register builds... with environment variables
        // http basic auth... single user
        // Run this in a openjdk:alpine image with apk add docker
        // Run a thread? with each build and stream the output somewhere
    }
}

private fun executeCommand(command: String, directory: String): String {
    println("Command: $command")
    val output = StringBuffer()
    try {
        val pb = ProcessBuilder(command.split(" "))
        pb.directory(File(directory))
        pb.redirectErrorStream(true)
        val p = pb.start()
        val exit = p.waitFor()
        println("Exit: $exit")
        val reader = BufferedReader(InputStreamReader(p.inputStream))
        var line = reader.readLine()
        while (line != null) {
            output.append(line + "\n")
            line = reader.readLine()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    val toString = output.toString()
    return toString
}
