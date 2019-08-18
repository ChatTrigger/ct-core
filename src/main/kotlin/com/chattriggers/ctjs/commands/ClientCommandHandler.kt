package com.chattriggers.ctjs.commands

import net.minecraft.command.*
import net.minecraft.command.CommandHandler
import net.minecraft.util.ChatComponentTranslation
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.EnumChatFormatting.*

/**
 * The class that handles client-side chat commands. You should register any
 * commands that you want handled on the client with this command handler.
 *
 * If there is a command with the same name registered both on the server and
 * client, the client takes precedence!
 *
 */
object ClientCommandHandler : CommandHandler() {

//    var latestAutoComplete: Array<String>? = null

    /**
     * @return 1 if successfully executed, -1 if no permission or wrong usage,
     * 0 if it doesn't exist or it was canceled (it's sent to the server)
     */
    /**
     * Attempt to execute a command. This method should return the number of times that the command was executed. If the
     * command does not exist or if the player does not have permission, 0 will be returned. A number greater than 1 can
     * be returned if a player selector is used.
     *
     * @param sender The person who executed the command. This could be an EntityPlayer, RCon Source, Command Block,
     * etc.
     * @param rawCommand The raw arguments that were passed. This includes the command name.
     */
    override fun executeCommand(sender: ICommandSender, message: String): Int {
        var message = message
        message = message.trim { it <= ' ' }

        if (message.startsWith("/")) {
            message = message.substring(1)
        }

        val temp = message.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val args = arrayOfNulls<String>(temp.size - 1)
        val commandName = temp[0]
        System.arraycopy(temp, 1, args, 0, args.size)
        val icommand = commands[commandName]

        try {
            if (icommand == null) {
                return 0
            }

            if (icommand.canCommandSenderUseCommand(sender)) {
                icommand.processCommand(sender, args)
                return 1
            } else {
                sender.addChatMessage(format(RED, "commands.generic.permission"))
            }
        } catch (wue: WrongUsageException) {
            sender.addChatMessage(format(RED, "commands.generic.usage", format(RED, wue.message ?: "", *wue.errorObjects)))
        } catch (ce: CommandException) {
            sender.addChatMessage(format(RED, ce.message ?: "", *ce.errorObjects))
        } catch (t: Throwable) {
            sender.addChatMessage(format(RED, "commands.generic.exception"))
            t.printStackTrace()
        }

        return -1
    }

    //Couple of helpers because the mcp names are stupid and long...
    private fun format(color: EnumChatFormatting, str: String, vararg args: Any): ChatComponentTranslation {
        val ret = ChatComponentTranslation(str, *args)
        ret.chatStyle.color = color
        return ret
    }

    fun removeCTCommands() {
        commands.entries.removeIf { it.value is Command }
    }
}