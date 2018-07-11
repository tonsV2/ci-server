package dk.fitfit.ci.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class CiServerApplication

fun main(args: Array<String>) {
    runApplication<CiServerApplication>(*args)
}

@RestController
@RequestMapping("/hooks")
class HookController {
    @PostMapping("/github")
    fun github(@RequestBody payload: Any) {
        println(payload.toString())
    }
}
