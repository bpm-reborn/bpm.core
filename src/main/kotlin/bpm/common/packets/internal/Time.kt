package bpm.common.packets.internal

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Represents a time value in milliseconds.
 *
 * This class provides properties to retrieve time components such as day, hour, minute, second, and millisecond.
 *
 * @property time The time value in milliseconds.
 * @constructor Creates an instance of [Time] with the given time value. If no value is provided, it defaults to 0.
 *
 * @property day The number of days in the time value.
 * @property hour The number of hours in the time value, ranging from 0 to 23.
 * @property minute The number of minutes in the time value, ranging from 0 to 59.
 * @property second The number of seconds in the time value, ranging from 0 to 59.
 * @property millisecond The number of milliseconds in the time value, ranging from 0 to 999.
 */
@JvmInline
value class Time(val time: Long = 0) {

    /**
     * Retrieves the number of hours from the given `time` value.
     *
     * The `time` value is represented in milliseconds since the epoch (January 1, 1970, 00:00:00 GMT).
     * This method calculates the number of hours by dividing the `time` value by 3,600,000 (the number of milliseconds in an hour),
     * and taking the modulus of 24 (to handle wrapping around).
     *
     * Converts to EST timezone.
     *
     * @return the number of hours
     */
    val hours: Int
        get() = ((time - 3600000 * 5) / 3600000).toInt()

    /**
     * Returns a boolean value indicating whether the time is in AM or not.
     *
     * @return true if the time is in AM, false otherwise.
     */
    val isAM: Boolean
        get() = (hours % 24) < 12

    /**
     * Represents the number of minutes in a given time.
     *
     * The `minute` property calculates the number of minutes based on the given `time` property.
     * It divides the `time` by 60000 to convert milliseconds to minutes, then takes the remainder
     * when divided by 60 to get the minutes in the current hour.
     *
     * @return The number of minutes in the given time.
     */
    val minute: Int
        get() = (time / 60000 % 60).toInt()

    /**
     * Represents the seconds component of a given time value.
     *
     * This property is calculated based on the given `time` value, which is expected to represent the total number of milliseconds.
     * The calculation extracts the seconds component by dividing the `time` value by 1000 and then using the modulo operator to get the remainder when divided by 60.
     *
     * @return The seconds component of the given `time` value as an integer.
     */
    val second: Int
        get() = (time / 1000 % 60).toInt()

    /**
     * Retrieves the milliseconds part of the time value.
     *
     * @return The milliseconds.
     */
    val millisecond: Int
        get() = (time % 1000).toInt()


    /**
     * Creates a time object that represents a future time based on the current time of the system.
     *
     * @param time The time value in milliseconds.
     * @return the current time in milliseconds added to the given time value.
     */
    fun future(time: Int): Time = Time(this.time + time)

    /**
     * Creates a time object that represents a future time based on the current time of the system.
     *
     * @param time The time value in seconds.
     * @return the current time in seconds added to the given time value.
     */
    fun futureSeconds(time: Int): Time = Time(this.time + time * 1000)

    /**
     * Creates a time object that represents a future time based on the current time of the system.
     *
     * @param time The time value in minutes.
     * @return the current time in minutes added to the given time value.
     */
    fun futureMinutes(time: Int): Time = Time(this.time + time * 60000)

    /**
     * Creates a time object that represents a future time based on the current time of the system.
     *
     * @param time The time value in hours.
     * @return the current time in hours added to the given time value.
     */
    fun futureHours(time: Int): Time = Time(this.time + time * 3600000)


    /**
     * Creates a time object that represents a past time based on the current time of the system.
     *
     * @param time The time value in milliseconds.
     * @return the current time in milliseconds subtracted by the given time value.
     */

    fun past(time: Int): Time = Time(this.time - time)

    /**
     * Creates a time object that represents a past time based on the current time of the system.
     *
     * @param time The time value in seconds.
     * @return the current time in seconds subtracted by the given time value.
     */

    fun pastSeconds(time: Int): Time = Time(this.time - time * 1000)

    /**
     * Creates a time object that represents a past time based on the current time of the system.
     *
     * @param time The time value in minutes.
     * @return the current time in minutes subtracted by the given time value.
     */

    fun pastMinutes(time: Int): Time = Time(this.time - time * 60000)

    /**
     * Creates a time object that represents a past time based on the current time of the system.
     *
     * @param time The time value in hours.
     * @return the current time in hours subtracted by the given time value.
     */

    fun pastHours(time: Int): Time = Time(this.time - time * 3600000)

    /**
     * Creates a time object that represents a past time based on the current time of the system.
     *
     * @param time The time value in days.
     * @return the current time in days subtracted by the given time value.
     */

    fun pastDays(time: Int): Time = Time(this.time - time * 86400000)
    /**
     * Determines if the specified time is before the given current time.
     *
     * @param now The current time to compare against.
     * @return Returns true if the specified time is before the current time, otherwise false.
     */
    fun isBefore(now: Time): Boolean = time < now.time

    /**
     * Determines if the specified time is after the given current time.
     *
     * @param now The current time to compare against.
     * @return Returns true if the specified time is after the current time, otherwise false.
     */
    fun isAfter(now: Time): Boolean = time > now.time
    operator fun minus(time: Time): Time = Time(this.time - time.time)
    operator fun minus(time: Long): Time = Time(this.time - time)

    operator fun plus(time: Time): Time = Time(this.time + time.time)

    operator fun compareTo(time: Time): Int = this.time.compareTo(time.time)

    operator fun compareTo(time: Long): Int = this.time.compareTo(time)

    operator fun compareTo(time: Int): Int = this.time.compareTo(time.toLong())

    override fun toString(): String = toString(false)


    /**
     * Finds the difference in*/
    infix fun until(time: Time): Time = Time(max(time.time, this.time) - min(time.time, this.time))


    fun toString(isDuration: Boolean): String {
        val hours = abs(hours % 24 - (if (!isDuration) 12 else 0))
        val minute = minute.toString().padStart(2, '0')
        val second = second.toString().padStart(2, '0')
        val millisecond = millisecond.toString().padStart(3, '0')
        return if (!isDuration) "Time($hours:$minute:$second ${if (isAM) "AM" else "PM"})"
        else "Time($minute:$second.$millisecond)"
    }
    @JvmInline
    value class Timer(val start: Time) {

        /**
         * Represents the time difference between the current time and the start time.
         *
         * The `delta` property calculates the time elapsed since the start time by subtracting the start time from the current time.
         * The result is stored as a `Time` object.
         *
         * @property delta The time difference between the current time and the start time.
         * @get The `delta` property retrieves the current time and subtracts the start time to calculate the elapsed time.
         * @return The calculated time difference as a `Time` object.
         *
         * @see Time
         */
        val delta: Time get() = now - start
        /**
         * The seconds since the timer started.
         *
         * @return The number of seconds elapsed.
         */
        val seconds: Int get() = delta.second

        /**
         * Variable representing the number of minutes in a given [`delta`](Duration) object.
         *
         * This variable retrieves the value of the `minute` component of the `delta` duration.
         *
         * @return The number of minutes in the `delta` duration.
         */
        val minutes: Int get() = delta.minute

        /**
         * Represents the number of hours in a specific time interval.
         *
         * @return The number of hours in the time interval.
         */
        val hours: Int get() = delta.hours

        override fun toString(): String = delta.toString(true)
    }


    companion object {

        /**
         * Represents the current time.
         *
         * The value of this variable is retrieved through the `get` property accessor, which returns a `Time` object
         * representing the current time in milliseconds.
         *
         * @property now Returns the current time as a `Time` object.
         */
        val now: Time
            get() = Time(System.currentTimeMillis())

        /**
         * Executes the given lambda function and measures the time taken.
         *
         * @param block the lambda function to be executed
         * @return the time taken in milliseconds
         */
        inline fun timed(block: Timer.() -> Unit): Time {
            val time = Time.Timer(now)
            time.block()
            return time.delta
        }


    }
}
