plugins { id("io.vacco.oss.gitflow") version "1.9.0" apply false }

subprojects {
  apply(plugin = "io.vacco.oss.gitflow")
  
  group = "io.vacco.ujgl.native"
  version = "0.1.0"

  configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
    sharedLibrary(true, false)
  }
}
