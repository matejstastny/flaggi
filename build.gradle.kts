tasks.register("meow") {
    group = "custom"
    description = "Debug task"
    doLast {
        println("meow")
    }
}
