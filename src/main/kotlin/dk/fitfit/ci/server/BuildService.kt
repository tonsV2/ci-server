package dk.fitfit.ci.server

import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.util.*

// TODO: Let buildService use buildContextService....

interface BuildService {
    fun getBuild(cloneUrl: String, ref: String): Build
    fun build(build: Build): Build
    fun save(build: Build): Build
}

@Service
class BuildServiceImpl(private val buildContextService: BuildContextService) : BuildService {
    // TODO: Return build with populated id
    override fun getBuild(cloneUrl: String, ref: String) = Build(context = buildContextService.getBuildContext(cloneUrl, ref))

    override fun save(build: Build): Build {
        return build
    }

    override fun build(build: Build): Build {
        // Create volume
        val volume = "ci-server-build-${build.id}"
        executeCommand("docker volume create --name=$volume", build)
        save(build)

        // Clone repo
        executeCommand("docker run --name ci-server-git-clone-${build.id} -t --rm -v $volume:/git alpine/git clone ${build.context.repository} .", build)
        save(build)

        // Git branch checkout
        executeCommand("docker run --name ci-server-git-clone-${build.id} -t --rm -v $volume:/git alpine/git checkout ${build.context.branch}", build)
        save(build)

        // Git reset
        executeCommand("docker run --name ci-server-git-reset-${build.id} -t --rm -v $volume:/git alpine/git reset --hard ${build.commitId}", build)
        save(build)

        // Mv .env.dist .env
        executeCommand("docker run --name ci-server-mv.env-${build.id} -t --rm -v $volume:/src -w /src alpine mv .env.dist .env", build) // TODO: if not .env and .env.dist or .env.sample or .env.example
        save(build)

        // Run our image on it
        val image = build.context.image
        val service = build.context.service
        val tag = when {
            build.context.branch == "master" -> "latest"
            else -> build.context.branch.replace("/", "_")
        }
        val registryUser = build.context.registryUser
        val registryPass = build.context.registryPass
        // TODO: Convert to docker compose...
        val command = "docker run --rm -t --name ci-server-worker-${build.id} -e SERVICE=$service -e TAG=$tag -e REGISTRY_USER=$registryUser -e REGISTRY_PASS=$registryPass -v $volume:/src -w /src -v /var/run/docker.sock:/var/run/docker.sock $image"
        executeCommand(command, build)
        save(build)

        // Rm volume
        executeCommand("docker volume rm $volume", build)
        save(build)

        build.buildEnd = LocalDateTime.now()
        save(build)
        return build
    }

    private fun executeCommand(command: String, build: Build, directory: String = "/tmp") {
        val cmd = BuildCommand(command)
        build.commands.add(cmd)

        val commandLog = "Command: $command"
        cmd.lines.add(BuildLine(commandLog))
        println(commandLog)
        try {
            val pb = ProcessBuilder(command.split(" "))
            pb.directory(File(directory))
            pb.redirectErrorStream(true)
            val p = pb.start()
            cmd.exit = p.waitFor()
            println("Exit: ${cmd.exit}") // TODO: Logs this to cmd.lines.add(BuildLine(...))
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            var line = reader.readLine()
            while (line != null) {
                println(line)
                cmd.lines.add(BuildLine(line))
                line = reader.readLine()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val done = "Command: Done!"
        println(done)
        cmd.lines.add(BuildLine(done))
    }

}

class Build(val id: String = UUID.randomUUID().toString(),
            val context: BuildContext,
            var payload: Any = "",
            var commitId: String = "",
            val buildStart: LocalDateTime = LocalDateTime.now(),
            var buildEnd: LocalDateTime? = null,
            val commands: MutableList<BuildCommand> = mutableListOf()
)

class BuildCommand(val command: String, var exit: Int = -1, val lines: MutableList<BuildLine> = mutableListOf()) {
    val log: String
        get() = lines.joinToString {
            "${it.line}\n"
        }
}

class BuildLine(val line: String, val timestamp: LocalDateTime = LocalDateTime.now(), val id: String = UUID.randomUUID().toString())
