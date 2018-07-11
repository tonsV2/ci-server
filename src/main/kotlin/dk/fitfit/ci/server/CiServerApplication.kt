package dk.fitfit.ci.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CiServerApplication

fun main(args: Array<String>) {
    runApplication<CiServerApplication>(*args)
}
