package bpm

import bpm.booostrap.Bootstrap
import net.neoforged.fml.common.Mod
import bpm.common.utils.ClassResourceScanner
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Mod(Bpm.ID)
object Bpm {

    const val ID = "bpm"
    internal val LOGGER: Logger = LogManager.getLogger(ID)
    @JvmStatic
    internal val bootstrap = Bootstrap(
        ClassResourceScanner
            .create()
            .scanSelf(true) //Only scan our current jar
            .scanClasspath(false) //Dont scan the classpath
            .withClassloader(Bpm::class.java.classLoader)
            .scan()
    ).collect()

    init {
        bootstrap.register()
    }

}

