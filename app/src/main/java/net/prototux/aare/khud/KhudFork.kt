package net.prototux.aare.khud

/**
 * KHUD fork: AARE is only the Android Auto link between the Carlinkit Pro2 and
 * the K20 Pro screen. The KHUD Carlink app (a Car App Library navigation app)
 * draws the whole picture: the mirrored Pro2 screen, its split panel and the
 * vehicle overlays. AARE opens it on connect and shows only its surface, with
 * no rail, template cards or action strip.
 */
object KhudFork {
    const val PACKAGE = "com.freezed23.khudcarlink.lab"

    fun isKhud(packageName: String?) = packageName == PACKAGE
}
