configure<io.vacco.oss.gitflow.GsPluginProfileExtension> {
  addJ8Spec()
}

tasks.withType<JavaCompile>().configureEach {
  options.compilerArgs.add("-Xlint:none")
  options.compilerArgs.add("-nowarn")
}

tasks.register<Exec>("generateBindings") {
  description = "Generate JNI bindings using jextract"
  group = "build"

  val scriptPath = projectDir.resolve("scripts/generate-bindings.sh")
  executable = "bash"
  args(scriptPath.absolutePath)
  doFirst {
    if (!scriptPath.exists()) {
      throw GradleException("Binding generation script not found: ${scriptPath.absolutePath}")
    }
  }
}

tasks.register<Exec>("buildNative") {
  description = "Build native libraries (GLFW + JNI wrapper)"
  group = "build"

  val scriptPath = projectDir.resolve("scripts/build-native.sh")
  executable = "bash"
  args(scriptPath.absolutePath)

  doFirst {
    if (!scriptPath.exists()) {
      throw GradleException("Native build script not found: ${scriptPath.absolutePath}")
    }
  }
}
