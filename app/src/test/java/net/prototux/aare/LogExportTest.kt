package net.prototux.aare

import org.junit.Assert.assertTrue
import org.junit.Test

class LogExportTest {
    @Test fun filenameIsSafe() {
        assertTrue(LogExport.fileName().matches(Regex("AARE_logs_[0-9]{8}_[0-9]{6}_[0-9]{3}\\.zip")))
    }
}
