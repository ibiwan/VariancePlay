import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

open class Hatchery<T : Animal>(private val type: KClass<T>) {
    fun fac(_name: String) = type.createInstance().apply { name = _name }
}

open class Animal(var name: String? = null) {
    override fun toString(): String = "${this.javaClass.name}: $name"

    companion object : Hatchery<Animal>(Animal::class)

    fun breathe() {}
}

open class Canid(name: String? = null) : Animal(name) {
    companion object : Hatchery<Canid>(Canid::class)

    fun howl() {}
}

open class Dog(name: String? = null) : Canid(name) {
    companion object : Hatchery<Dog>(Dog::class)

    fun heel() {}
}

// (1) DECLARATION-SITE COVARIANCE (class is producer)
class Breeder<out T : Animal>(val fac: (String) -> T) {
    fun make(name: String): List<T> = listOf(fac(name))
}

// (2) DECLARATION-SITE CONTRAVARIANCE (class is consumer)
class Collector<in T : Animal>(private val collection: MutableList<T> = mutableListOf()) {
    val size: Int get() = collection.size
    fun forEach(fName: String) = collection.forEach {
        when (fName) {
            "breathe" -> (it as Animal).breathe()
            "howl" -> (it as Canid).howl()
            "heel" -> (it as Dog).heel()
        }
    }

    fun addAll(donations: List<T>) {
        collection += donations
    }

    fun orderFrom(name: String, breeder: Breeder<T>) = addAll(breeder.make(name))

    // (bonus) USE-SITE CONTRAVARIANCE (other is producer)
    fun addAllFrom(other: Zoo<out T>) = other.forEach { collection.add(it) }
}

class Zoo<T : Animal> {
    private val denizens = mutableListOf<T>()
    fun forEach(f: (T) -> Unit) = denizens.forEach { f(it) }

    fun addAll(transfers: List<T>) = denizens.addAll(transfers)
    val size: Int get() = denizens.size

    // (3) USE-SITE COVARIANCE (other is consumer)
    fun sendAllTo(other: Zoo<in T>) = other.addAll(denizens)

    // (4) USE-SITE CONTRAVARIANCE (other is producer)
    fun getAllFrom(other: Zoo<out T>): Zoo<T> = this.apply { addAll(other.denizens) }

    // (5) IMPLICIT COVARIANCE (see (2))
    fun sendAllTo(other: Collector<T>) = other.addAll(denizens)

    // (6) IMPLICIT CONTRAVARIANCE (see (1))
    fun orderFrom(name: String, breeder: Breeder<T>) = addAll(breeder.make(name))
}

fun main() {

}