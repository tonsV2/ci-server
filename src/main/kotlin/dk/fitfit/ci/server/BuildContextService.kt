package dk.fitfit.ci.server

import org.springframework.stereotype.Service

interface BuildContextService {
    fun getBuildContext(cloneUrl: String, ref: String): BuildContext
}

@Service
class BuildContextServiceImpl : BuildContextService {
    override fun getBuildContext(cloneUrl: String, ref: String): BuildContext {
        // TODO: Search for build context in db... If no context... Create one?
        val buildContext = BuildContext()
        buildContext.image = "tons/dc-ci"
        buildContext.service = "release"
        buildContext.registryUser = "tons"
        buildContext.registryPass = "skummet"
        buildContext.repository = cloneUrl
        buildContext.ref = ref
        return buildContext
    }
}

// ** Build context...
//source... git... user/pass... ssh
//condition... any valid jsonpath? Could possible point to a branch
//What to build: Service
//Tag...
//artifact storage... user, pass, host
class BuildContext(var image: String = "",
                   var service: String = "",
                   var registryUser: String = "",
                   var registryPass: String = "",
                   var repository: String = "",
                   var ref: String = "") {
    val branch: String
        get() = ref.removePrefix("refs/heads/")
}
