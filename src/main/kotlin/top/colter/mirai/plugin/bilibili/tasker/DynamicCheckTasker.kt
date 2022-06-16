package top.colter.mirai.plugin.bilibili.tasker

import top.colter.mirai.plugin.bilibili.BiliBiliDynamic
import top.colter.mirai.plugin.bilibili.BiliConfig
import top.colter.mirai.plugin.bilibili.BiliData
import top.colter.mirai.plugin.bilibili.api.getNewDynamic
import top.colter.mirai.plugin.bilibili.client.BiliClient
import top.colter.mirai.plugin.bilibili.data.DynamicDetail
import top.colter.mirai.plugin.bilibili.data.DynamicType
import top.colter.mirai.plugin.bilibili.utils.sendAll
import top.colter.mirai.plugin.bilibili.utils.time
import java.time.Instant

object DynamicCheckTasker : BiliTasker() {

    override val interval: Int = BiliConfig.checkConfig.interval

    private val dynamicChannel by BiliBiliDynamic::dynamicChannel

    private val dynamic by BiliData::dynamic

    private val listenAllDynamicMode = true

    private val client = BiliClient()

    private val banType = listOf(
        DynamicType.DYNAMIC_TYPE_LIVE,
        DynamicType.DYNAMIC_TYPE_LIVE_RCMD,
        DynamicType.DYNAMIC_TYPE_PGC
    )

    private var lastDynamic: Long = Instant.now().epochSecond

    override suspend fun main() {
        logger.debug("Check Dynamic...")
        val dynamicList = client.getNewDynamic()
        if (dynamicList != null) {
            //logger.info(dynamicList.updateBaseline)
            val dynamics = dynamicList.items
                .filter {
                    !banType.contains(it.type)
                }.filter {
                    it.time > lastDynamic
                }.filter {
                    if (listenAllDynamicMode) {
                        true
                    } else {
                        dynamic.filter { it.value.contacts.isNotEmpty() }.map { it.key }
                            .contains(it.modules.moduleAuthor.mid)
                    }
                }.sortedBy {
                    it.time
                }

            if (dynamics.isNotEmpty()) lastDynamic = dynamics.last().time
            dynamicChannel.sendAll(dynamics.map { DynamicDetail(it) })
        }
    }

}