package top.colter.mirai.plugin.bilibili.command

import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSenderOnMessage
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.descriptor.CommandArgumentParserException
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.utils.ExternalResource.Companion.sendAsImageTo
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import top.colter.mirai.plugin.bilibili.BiliBiliDynamic
import top.colter.mirai.plugin.bilibili.BiliBiliDynamic.crossContact
import top.colter.mirai.plugin.bilibili.BiliConfig
import top.colter.mirai.plugin.bilibili.FilterMode
import top.colter.mirai.plugin.bilibili.FilterType
import top.colter.mirai.plugin.bilibili.api.getDynamicDetail
import top.colter.mirai.plugin.bilibili.api.getLive
import top.colter.mirai.plugin.bilibili.api.getUserNewDynamic
import top.colter.mirai.plugin.bilibili.data.DynamicDetail
import top.colter.mirai.plugin.bilibili.data.LiveDetail
import top.colter.mirai.plugin.bilibili.tasker.BiliDataTasker
import top.colter.mirai.plugin.bilibili.utils.*

object DynamicCommand : CompositeCommand(
    owner = BiliBiliDynamic,
    "bili",
    description = "动态指令"
) {

    @SubCommand("h", "help", "帮助", "menu")
    suspend fun CommandSender.help() {
        loadResourceBytes("image/HELP.png").toExternalResource().toAutoCloseable().sendAsImageTo(Contact())
    }

    @SubCommand("color", "颜色")
    suspend fun CommandSender.color(user: String, color: String) {
        matchUser(user) {
            BiliDataTasker.setColor(it, color)
        }?.let {
            sendMessage(it)
            actionNotify(this.subject?.id, ActionMessage(name, user, "修改主题色", it))
        }
    }

    @SubCommand("add", "添加", "订阅")
    suspend fun CommandSender.add(uid: Long, contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法为其他人添加订阅")
            return
        }
        val msg = BiliDataTasker.addSubscribe(uid, contact.delegate)
        sendMessage(msg)
        actionNotify(this.subject?.id, ActionMessage(name, contact.name, "订阅", msg))
    }

    @SubCommand("del", "删除")
    suspend fun CommandSender.del(user: String, contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法删除其他人的订阅")
            return
        }
        matchUser(user) {
            BiliDataTasker.removeSubscribe(it, contact.delegate).let { it1 -> "对 ${it1?.name} 取消订阅成功!" }
        }?.let {
            sendMessage(it)
            actionNotify(this.subject?.id, ActionMessage(name, contact.name, "取消订阅", it))
        }
    }

    @SubCommand("delAll", "删除全部订阅")
    suspend fun CommandSender.delAll(contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法删除其他人的订阅")
            return
        }
        val msg = BiliDataTasker.removeAllSubscribe(contact.delegate).let { "删除订阅成功! 共删除 $it 个订阅" }
        sendMessage(msg)
        actionNotify(this.subject?.id, ActionMessage(name, contact.name, "取消全部订阅", msg))
    }

    @SubCommand("list", "列表")
    suspend fun CommandSender.list(contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法查看其他人的订阅")
            return
        }
        sendMessage(BiliDataTasker.list(contact.delegate))
    }

    @SubCommand("listAll", "la", "全部订阅列表")
    suspend fun CommandSender.listAll() {
        if (BiliConfig.admin == Contact().id || BiliConfig.admin == user?.id)
            sendMessage(BiliDataTasker.listAll())
        else sendMessage("仅bot管理员可获取")
    }

    @SubCommand("listUser", "lu", "用户列表")
    suspend fun CommandSender.listUser() {
        if (BiliConfig.admin == Contact().id || BiliConfig.admin == user?.id)
            sendMessage(BiliDataTasker.listUser())
        else sendMessage("仅bot管理员可获取")
    }

    @SubCommand("filterMode", "fm", "过滤模式")
    suspend fun CommandSender.filterMode(type: String, mode: String, uid: Long = 0L, contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法修改其他人的过滤模式")
            return
        }
        sendMessage(
            BiliDataTasker.addFilter(
                if (type == "t") FilterType.TYPE else FilterType.REGULAR,
                if (mode == "w") FilterMode.WHITE_LIST else FilterMode.BLACK_LIST,
                null, uid, contact.delegate
            )
        )
    }

    @SubCommand("filterType", "ft", "类型过滤")
    suspend fun CommandSender.filterType(type: String, uid: Long = 0L, contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法为其他人添加过滤器")
            return
        }
        sendMessage(BiliDataTasker.addFilter(FilterType.TYPE, null, type, uid, contact.delegate))
    }

    @SubCommand("filterReg", "fr", "正则过滤")
    suspend fun CommandSender.filterReg(reg: String, uid: Long = 0L, contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法为其他人添加过滤器")
            return
        }
        sendMessage(BiliDataTasker.addFilter(FilterType.REGULAR, null, reg, uid, contact.delegate))
    }

    @SubCommand("filterList", "fl", "过滤列表")
    suspend fun CommandSender.filterList(uid: Long = 0L, contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法查看其他人的过滤器")
            return
        }
        sendMessage(BiliDataTasker.listFilter(uid, contact.delegate))
    }

    @SubCommand("filterDel", "fd", "过滤删除")
    suspend fun CommandSender.filterDel(index: String, uid: Long = 0L, contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法删除其他人的过滤器")
            return
        }
        sendMessage(BiliDataTasker.delFilter(index, uid, contact.delegate))
    }

    @SubCommand("templateList", "tl", "模板列表")
    suspend fun CommandSenderOnMessage<*>.templateList() {
        BiliDataTasker.listTemplate("d", Contact())
        BiliDataTasker.listTemplate("l", Contact())
    }

    @SubCommand("template", "t", "模板")
    suspend fun CommandSender.template(type: String, template: String, contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法修改其他人的模板")
            return
        }
        sendMessage(BiliDataTasker.setTemplate(type, template, contact))
    }

    @SubCommand("login", "登录")
    suspend fun CommandSenderOnMessage<*>.login() {
        val subject = Contact()
        if (BiliConfig.admin == subject.id || BiliConfig.admin == user?.id)
            BiliDataTasker.login(subject)
        else sendMessage("仅bot管理员可进行登录")
    }

    @SubCommand("atall", "aa", "at全体")
    suspend fun CommandSender.atall(type: String = "a", user: String = "0", contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法修改其他人的配置")
            return
        }
        matchUser(user){
            BiliDataTasker.addAtAll(type, it, contact)
        }?.let { sendMessage(it) }
    }

    @SubCommand("delAtall", "daa", "取消at全体")
    suspend fun CommandSender.delAtall(type: String = "a", user: String = "0", contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法修改其他人的配置")
            return
        }
        matchUser(user){
            BiliDataTasker.delAtAll(type, it, contact)
        }?.let { sendMessage(it) }
    }

    @SubCommand("listAtall", "laa", "at全体列表")
    suspend fun CommandSender.listAtall(user: String = "0", contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法查看其他人的配置")
            return
        }
        matchUser(user){
            BiliDataTasker.listAtAll(it, contact)
        }?.let { sendMessage(it) }
    }

    @SubCommand("config", "配置")
    suspend fun CommandSenderOnMessage<*>.config(user: String = "0", contact: Contact = Contact()) {
        if (!hasPermission(crossContact) && contact.delegate != Contact().delegate) {
            sendMessage("权限不足, 无法修改其他人的配置")
            return
        }
        if (user == "0") {
            BiliDataTasker.config(fromEvent, 0, contact)
        }else {
            matchUser(user){
                BiliDataTasker.config(fromEvent, it, contact)
                null
            }?.let { sendMessage(it) }
        }
    }

    @SubCommand("search", "s", "搜索")
    suspend fun CommandSenderOnMessage<*>.search(did: String) {
        val subject = Contact()
        val detail = biliClient.getDynamicDetail(did)
        if (detail != null) subject.sendMessage("少女祈祷中...") else subject.sendMessage("未找到动态")
        detail?.let { d -> BiliBiliDynamic.dynamicChannel.send(DynamicDetail(d, subject.delegate)) }
    }

    @SubCommand("live", "直播")
    suspend fun CommandSenderOnMessage<*>.live() {
        val subject = Contact()
        val detail = biliClient.getLive(1, 1)
        if (detail != null) subject.sendMessage("少女祈祷中...") else subject.sendMessage("当前没有人在直播")
        detail?.let { d -> BiliBiliDynamic.liveChannel.send(LiveDetail(d.rooms.first(), subject.delegate)) }
    }

    @SubCommand("new", "最新动态")
    suspend fun CommandSenderOnMessage<*>.new(user: String, count: Int = 1) {
        matchUser(user){
            val list = biliClient.getUserNewDynamic(it)?.items?.subList(0, count)
            list?.forEach { di ->
                BiliBiliDynamic.dynamicChannel.send(DynamicDetail(di, Contact().delegate))
            }
            if (list != null && list.isNotEmpty()) "少女祈祷中..." else "未找到动态"
        }?.let { sendMessage(it) }
    }

}

fun CommandSender.Contact(): Contact = subject ?: throw CommandArgumentParserException("无法从当前环境获取联系人")