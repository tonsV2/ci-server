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

// TODO: Version 2.0
// http basic auth... single user

@RestController
@RequestMapping("/hooks")
class HookController(private val buildService: BuildService) {
    @PostMapping("/github")
    fun github(@RequestBody payload: LinkedHashMap<String, *>) {
        val ref = payload["ref"].toString()

        val repository = payload["repository"]
        var cloneUrl = ""
        if (repository is LinkedHashMap<*, *>) {
            cloneUrl = repository.get("clone_url").toString()
        }

        val headCommit = payload["head_commit"]
        var commitId = ""
        if (headCommit is LinkedHashMap<*, *>) {
            commitId = headCommit.get("id").toString()
        }

        val build = buildService.getBuild(cloneUrl, ref)
        thread {
            build.commitId = commitId
            build.payload = payload
            buildService.build(build)
        }
    }

    @PostMapping("/bitbucket")
    fun bitbucket(@RequestBody payload: Any) {
        val mapper = ObjectMapper()
        val json = mapper.writeValueAsString(payload)
        println(json)
    }
}

@RestController
class BuildController {
    @GetMapping("/builds/{id}")
    fun build(@PathVariable id: String) {
        TODO("Return build details")
    }

    @GetMapping("/builds")
    fun builds(): String {
        TODO("Return list of builds")
    }
}
