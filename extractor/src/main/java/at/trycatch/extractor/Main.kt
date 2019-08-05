package at.trycatch.extractor

fun main(args: Array<String>) {
    if (args.size != 1) {
        throw IllegalArgumentException("You need to provide the city as (only) argument.")
    }

    val city = args[0]

    println("Extracting data for `$city`...")

    Extractor(city).start()
}

fun String.getNormalized(): String {
    return toLowerCase()
            .replace("st.", "sankt")
            .replace("prof.", "professor")
            .replace(".", "")
            .replace(" ", "")
            .replace("-", "")
}