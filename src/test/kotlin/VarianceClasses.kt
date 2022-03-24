import org.amshove.kluent.*
import org.junit.Test

@Suppress("UNCHECKED_CAST", "UNUSED_EXPRESSION", "PrivatePropertyName")
class VarianceTests {
    private val animals = listOf(Animal("bird"), Animal("snake"))
    private val canids = listOf(Canid("wolf"), Canid("coyote"))
    private val dogs = listOf(Dog("shepherd"), Dog("labrador"))

    data class TestSetup(
        val animalZoo: Zoo<Animal>,
        val canidZoo: Zoo<Canid>,
        val dogZoo: Zoo<Dog>,
    )

    private fun setup() = TestSetup(
        animalZoo = Zoo<Animal>().apply { addAll(animals) },
        canidZoo = Zoo<Canid>().apply { addAll(canids) },
        dogZoo = Zoo<Dog>().apply { addAll(dogs) },
    )

    @Test
    // ok by default
    fun `zoos can SEND TO SAME-TYPE (invariant) zoos`() {
        val canidZoo = setup().canidZoo
        val canidZooStartSize = canidZoo.size
        canidZooStartSize shouldBeGreaterThan 0

        canidZoo.sendAllTo(canidZoo)
        canidZoo.size shouldBeEqualTo canidZooStartSize + canidZooStartSize
        canidZoo.forEach { it.howl() }
    }

    @Test
    // enabled by (3) `fun sendAllTo(other: Zoo<in T>)`
    fun `zoos can SEND TO SUPERTYPE (contravariant) zoos`() {
        val (
            animalZoo,
            canidZoo,
            dogZoo,
        ) = setup()
        val animalZooStartSize = animalZoo.size
        val canidZooStartSize = canidZoo.size
        val dogZooStartSize = dogZoo.size

        animalZooStartSize shouldBeGreaterThan 0
        canidZooStartSize shouldBeGreaterThan 0
        dogZooStartSize shouldBeGreaterThan 0

        canidZoo.sendAllTo(animalZoo)
        dogZoo.sendAllTo(animalZoo)
        animalZoo.size shouldBeEqualTo animalZooStartSize + canidZooStartSize + dogZooStartSize
        animalZoo.forEach { it.breathe() }

        dogZoo.sendAllTo(canidZoo)
        canidZoo.size shouldBeEqualTo canidZooStartSize + dogZooStartSize
        canidZoo.forEach { it.howl() }
    }

    @Test
    // forbidden by (3) `fun sendAllTo(other: Zoo<in T>)`
    fun `zoos can NOT SEND TO SUBTYPE (covariant) zoos`() {
        val (
            animalZoo,
            canidZoo,
            dogZoo,
        ) = setup()

        animalZoo.sendAllTo(dogZoo as Zoo<Animal>)
        invoking {
            dogZoo.forEach { (it as Dog).heel() }
        } shouldThrow ClassCastException::class

        (animalZoo as Zoo<Canid>).sendAllTo(canidZoo)
        invoking {
            canidZoo.forEach { it.howl() }
        } shouldThrow ClassCastException::class

    }

    @Test
    // ok by default
    fun `zoos can RECEIVE FROM SAME-TYPE (invariant) zoos`() {
        val canidZoo = setup().canidZoo
        val canidZooStartSize = canidZoo.size
        canidZooStartSize shouldBeGreaterThan 0

        canidZoo.getAllFrom(canidZoo)
        canidZoo.size shouldBeEqualTo canidZooStartSize + canidZooStartSize
        canidZoo.forEach { it.howl() }
    }

    @Test
    // enabled by (4) `fun getAllFrom(other: Zoo<out T>): Zoo<T>`
    fun `zoos can RECEIVE FROM SUBTYPE (covariant) zoos`() {
        val (
            animalZoo,
            canidZoo,
            dogZoo,
        ) = setup()
        val animalZooStartSize = animalZoo.size
        val canidZooStartSize = canidZoo.size
        val dogZooStartSize = dogZoo.size

        animalZooStartSize shouldBeGreaterThan 0
        canidZooStartSize shouldBeGreaterThan 0
        dogZooStartSize shouldBeGreaterThan 0

        animalZoo.getAllFrom(canidZoo)
        animalZoo.getAllFrom(dogZoo)
        animalZoo.size shouldBeEqualTo animalZooStartSize + canidZooStartSize + dogZooStartSize
        animalZoo.forEach { it.breathe() }

        canidZoo.getAllFrom(dogZoo)
        canidZoo.size shouldBeEqualTo canidZooStartSize + dogZooStartSize
        canidZoo.forEach { it.howl() }
    }

    @Test
    // forbidden by (4) `fun getAllFrom(other: Zoo<out T>): Zoo<T>`
    fun `zoos can NOT RECEIVE FROM SUPERTYPE (contravariant) zoos`() {
        val (
            animalZoo,
            canidZoo,
            dogZoo,
        ) = setup()

        dogZoo.getAllFrom(animalZoo as Zoo<Dog>)
        invoking {
            dogZoo.forEach { it.heel() }
        } shouldThrow ClassCastException::class

        (canidZoo as Zoo<Animal>).getAllFrom(animalZoo)
        invoking {
            canidZoo.forEach { (it as Canid).howl() }
        } shouldThrow ClassCastException::class
    }

    @Test
    // ok by default
    fun `collectors CAN ORDER FROM SAME-TYPE (invariant) breeders`() {
        val animalCollector = Collector<Animal>()
        val canidCollector = Collector<Canid>()
        val dogCollector = Collector<Dog>()

        val animalBreeder = Breeder(Animal::fac)
        val canidBreeder = Breeder(Canid::fac)
        val dogBreeder = Breeder(Dog::fac)

        animalCollector.size shouldBeEqualTo 0
        animalCollector.orderFrom("eagle", animalBreeder)
        animalCollector.size shouldBeEqualTo 1

        canidCollector.size shouldBeEqualTo 0
        canidCollector.orderFrom("fox", canidBreeder)
        canidCollector.size shouldBeEqualTo 1

        dogCollector.size shouldBeEqualTo 0
        dogCollector.orderFrom("dane", dogBreeder)
        dogCollector.size shouldBeEqualTo 1
    }

    @Test
    // enabled by (1) `class Breeder<out T>`
    fun `collectors CAN ORDER FROM SUBTYPE (covariant) breeders`() {
        val animalCollector = Collector<Animal>()
        val canidCollector = Collector<Canid>()

        val canidBreeder = Breeder(Canid::fac)
        val dogBreeder = Breeder(Dog::fac)

        animalCollector.size shouldBeEqualTo 0
        animalCollector.orderFrom("dane", dogBreeder)
        animalCollector.orderFrom("fox", canidBreeder)
        animalCollector.size shouldBeEqualTo 2

        canidCollector.size shouldBeEqualTo 0
        canidCollector.orderFrom("dane", dogBreeder)
        canidCollector.size shouldBeEqualTo 1
    }

    @Test
    // forbidden by (1) `class Breeder<out T>`
    fun `collectors CAN NOT ORDER FROM SUPERTYPE (contravariant) breeders`() {
        val canidCollector = Collector<Canid>()
        val dogCollector = Collector<Dog>()

        val animalBreeder = Breeder(Animal::fac)
        val canidBreeder = Breeder(Canid::fac)

        dogCollector.orderFrom("fox", (canidBreeder as Breeder<Dog>))
        invoking {
            dogCollector.forEach("heel")
        } shouldThrow ClassCastException::class

        (canidCollector as Collector<Animal>).orderFrom("eagle", animalBreeder)
        invoking {
            canidCollector.forEach("howl")
        } shouldThrow ClassCastException::class
    }

    @Test
    // ok by default
    fun `zoos CAN SEND TO SAME-TYPE (invariant) collectors`() {
        val canidZoo = setup().canidZoo
        val canidCollector = Collector<Canid>()

        canidCollector.size shouldBeEqualTo 0
        canidZoo.sendAllTo(canidCollector)
        canidCollector.size shouldBeEqualTo canidZoo.size
    }

    @Test
    // enabled by (2) `class Collector<in T>`
    fun `zoos CAN SEND TO SUPERTYPE (contravariant) collectors`() {
        val (
            _,
            canidZoo,
            dogZoo,
        ) = setup()
        val animalCollector = Collector<Animal>()
        val canidCollector = Collector<Canid>()

        animalCollector.size shouldBeEqualTo 0
        canidZoo.sendAllTo(animalCollector)
        dogZoo.sendAllTo(animalCollector)
        animalCollector.size shouldBeEqualTo canidZoo.size + dogZoo.size

        canidCollector.size shouldBeEqualTo 0
        dogZoo.sendAllTo(canidCollector)
        canidCollector.size shouldBeEqualTo dogZoo.size
    }

    @Test
    // forbidden by (2) `class Collector<in T>`
    fun `zoos CAN NOT SEND TO SUBTYPE (covariant) collectors`() {
        val (
            animalZoo,
            canidZoo,
            _,
        ) = setup()

        val canidCollector = Collector<Canid>()
        val dogCollector = Collector<Dog>()

        canidZoo.sendAllTo(dogCollector as Collector<Canid>)
        invoking {
            dogCollector.forEach("heel")
        } shouldThrow ClassCastException::class

        (animalZoo as Zoo<Canid>).sendAllTo(canidCollector)
        invoking {
            canidCollector.forEach("howl")
        } shouldThrow ClassCastException::class
    }

    @Test
    // ok by default
    fun `collectors can RECEIVE ALL FROM SAME-TYPE (invariant) zoos`() {
        val canidZoo = setup().canidZoo
        val canidCollector = Collector<Canid>()

        canidCollector.size shouldBeEqualTo 0
        canidCollector.addAllFrom(canidZoo)
        canidCollector.size shouldBeEqualTo canidZoo.size
    }

    @Test
    // enabled by (bonus) `fun addAllFrom(other: Zoo<out T>)`
    fun `collectors can RECEIVE ALL FROM SUBTYPE (covariant) zoos`() {
        val (
            _,
            canidZoo,
            dogZoo,
        ) = setup()
        val animalCollector = Collector<Animal>()
        val canidCollector = Collector<Canid>()

        animalCollector.size shouldBeEqualTo 0
        animalCollector.addAllFrom(canidZoo)
        animalCollector.addAllFrom(dogZoo)
        animalCollector.size shouldBeEqualTo canidZoo.size + dogZoo.size

        canidCollector.size shouldBeEqualTo 0
        canidCollector.addAllFrom(dogZoo)
        canidCollector.size shouldBeEqualTo dogZoo.size
    }

    @Test
    // forbidden by (bonus) `fun addAllFrom(other: Zoo<out T>)`
    fun `collectors can NOT RECEIVE ALL FROM SUPERTYPE (contravariant) zoos`() {
        val (
            animalZoo,
            canidZoo,
            _,
        ) = setup()

        val canidCollector = Collector<Canid>()
        val dogCollector = Collector<Dog>()

        dogCollector.addAllFrom(canidZoo as Zoo<Dog>)
        invoking {
            dogCollector.forEach("heel")
        } shouldThrow ClassCastException::class

        (canidCollector as Collector<Animal>).addAllFrom(animalZoo)
        invoking {
            canidCollector.forEach("howl")
        } shouldThrow ClassCastException::class
    }

    @Test
    // ok by default
    fun `zoos CAN ORDER FROM SAME-TYPE (invariant) breeders`() {
        val (
            animalZoo,
            canidZoo,
            dogZoo,
        ) = setup()

        val animalBreeder = Breeder(Animal::fac)
        val canidBreeder = Breeder(Canid::fac)
        val dogBreeder = Breeder(Dog::fac)

        val animalZooStartSize = animalZoo.size
        animalZoo.orderFrom("eagle", animalBreeder)
        animalZoo.size shouldBeEqualTo animalZooStartSize + 1
        animalZoo.forEach { it.breathe() }

        val canidZooStartSize = canidZoo.size
        canidZoo.orderFrom("fox", canidBreeder)
        canidZoo.size shouldBeEqualTo canidZooStartSize + 1
        canidZoo.forEach { it.howl() }

        val dogZooStartSize = dogZoo.size
        dogZoo.orderFrom("dane", dogBreeder)
        dogZoo.size shouldBeEqualTo dogZooStartSize + 1
        dogZoo.forEach { it.heel() }
    }

    @Test
    // enabled by (1) `class Breeder<out T>`
    fun `zoos CAN ORDER FROM SUBTYPE (covariant) breeders`() {
        val (
            animalZoo,
            canidZoo,
        ) = setup()

        val canidBreeder = Breeder(Canid::fac)
        val dogBreeder = Breeder(Dog::fac)

        val animalZooStartSize = animalZoo.size
        animalZoo.orderFrom("fox", canidBreeder)
        animalZoo.orderFrom("dane", dogBreeder)
        animalZoo.size shouldBeEqualTo animalZooStartSize + 2
        animalZoo.forEach { it.breathe() }

        val canidZooStartSize = canidZoo.size
        canidZoo.orderFrom("dane", dogBreeder)
        canidZoo.size shouldBeEqualTo canidZooStartSize + 1
    }

    @Test
    // forbidden by (1) `class Breeder<out T>`
    fun `zoos CAN NOT ORDER FROM SUPERTYPE (contravariant) breeders`() {
        val (
            _,
            canidZoo,
            dogZoo,
        ) = setup()

        val animalBreeder = Breeder(Animal::fac)
        val canidBreeder = Breeder(Canid::fac)

        dogZoo.orderFrom("fox", canidBreeder as Breeder<Dog>)
        invoking {
            dogZoo.forEach { it.heel() }
        } shouldThrow ClassCastException::class

        canidZoo.orderFrom("eagle", animalBreeder as Breeder<Canid>)
        invoking {
            canidZoo.forEach { it.howl() }
        } shouldThrow ClassCastException::class
    }

    // add six more for breeders to actively deliver?
}
