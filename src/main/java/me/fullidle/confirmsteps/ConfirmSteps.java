package me.fullidle.confirmsteps;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ConfirmSteps extends JavaPlugin implements CommandExecutor, TabExecutor {
    Map<UUID, List<String>> map = new HashMap<>();
    static String[] help = new String[]{
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "§3<csteps>->HELP",
            "§7- set [player] [指令内容(使用配置中的符号分割开多个指令)]",
            "§7  设置玩家待执行内容",
            "",
            "§7- execute [player]",
            "§7  执行待执行命令(会根据配置自动删除)",
            "",
            "§7- del [player]",
            "§7  删除玩家待执行内容",
            "",
            "§7- reload",
            "§7  重载配置",
            "",
            "§7- see [player]",
            "§7  查看玩家待执行命令"

    };
    static String[] subCmd = new String[]{
            "help","set","del","execute","reload","see"
    };

    @Override
    public void onEnable() {
        // Plugin startup logic
        PluginCommand command = getCommand(getName().toLowerCase());
        assert command != null;
        command.setExecutor(this);
        command.setTabCompleter(this);
        getLogger().info("§3插件§b已载入!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            String sub = args[0];
            if (Arrays.asList(subCmd).contains(sub)){
                Permission permission = this.getServer().getPluginManager().getPermission(this.getName().toLowerCase()+"."+sub);
                if (permission != null){
                    if (!hasPermissionM(sender,permission)) {
                        return false;
                    }
                }
                switch (sub) {
                    case "help":
                    {
                        break;
                    }
                    case "set":
                    {
                        if (args.length < 3){
                            sender.sendMessage("§c参数不全");
                            break;
                        }
                        Player player = Bukkit.getPlayer(args[1]);
                        StringBuilder builder = new StringBuilder();
                        for (int i = 2; i < args.length; i++) {
                            String arg = args[i];
                            builder.append(arg).append(" ");
                        }
                        String data = builder.toString();
                        if (!isOnline(sender,player)) {
                            return false;
                        }
                        String separator = getConfig().getString("separator");
                        assert separator != null;
                        String[] split = data.split(separator);
                        ArrayList<String> arrayList = new ArrayList<>();
                        for (String s : split)
                            arrayList.add(s.replace(separator,""));
                        assert player != null;
                        map.remove(player.getUniqueId());
                        map.put(player.getUniqueId(),arrayList);
                        sender.sendMessage("§a设置待执行数据成功!");
                        return false;
                    }
                    case "execute":
                    {
                        if (args.length < 2){
                            sender.sendMessage("§c参数不全");
                            break;
                        }
                        String pName = args[1];
                        Player player = Bukkit.getPlayer(pName);
                        if (!isOnline(sender,player)){
                            return false;
                        }
                        assert player != null;
                        UUID uuid = player.getUniqueId();
                        List<String> strings = map.get(uuid);
                        if (strings == null){
                            sender.sendMessage("§c该玩家没有待执行数据");
                            return false;
                        }
                        if (getConfig().getBoolean("autoDel")){
                            map.remove(uuid);
                        }
                        String statusSymbol = getConfig().getString("statusSymbol");
                        assert statusSymbol != null;
                        char a = statusSymbol.charAt(0);
                        char b = statusSymbol.charAt(1);
                        String regex = "\\{([^}]+)\\}";
                        Pattern pattern = Pattern.compile(regex);
                        int x = 0;
                        for (String d : strings) {
                            Matcher matcher = pattern.matcher(d);
                            if (matcher.find()) {
                                String i = matcher.group(1);
                                String c = d.replace(a + i + b, "");
                                executeCmd(sender,i,c);
                            }else{
                                x++;
                                getServer().dispatchCommand(sender,d);
                            }
                        }
                        sender.sendMessage("§a已执行§c"+x+"§a默认身份的指令和§6"+(strings.size() - x)+"§a条指定身份的指令");
                        return false;
                    }
                    case "del":
                    {
                        if (args.length < 2){
                            sender.sendMessage("§c参数不全");
                            break;
                        }
                        String pName = args[1];
                        Player player = Bukkit.getPlayer(pName);
                        if (!isOnline(sender,player)) {
                            return false;
                        }
                        assert player != null;
                        map.remove(player.getUniqueId());
                        sender.sendMessage("§a删除待执行数据成功!");
                        return false;
                    }
                    case "reload":
                    {
                        saveDefaultConfig();
                        reloadConfig();
                        sender.sendMessage("§a重载成功");
                        return false;
                    }
                    case "see":
                    {
                        if (args.length < 2){
                            sender.sendMessage("§c参数不全");
                            break;
                        }
                        String pName = args[1];
                        Player player = Bukkit.getPlayer(pName);
                        if (!isOnline(sender,player)) {
                            return false;
                        }
                        assert player != null;
                        List<String> strings = map.get(player.getUniqueId());
                        if (strings == null){
                            sender.sendMessage("§c该玩家没有数据");
                            return false;
                        }
                        sender.sendMessage(strings.toArray(new String[]{}));
                        return false;
                    }
                }
            }
        }
        sender.sendMessage(help);
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = Arrays.asList(subCmd);
        if (args.length < 1){
            return list;
        }
        if (args.length == 1){
            return list.stream().filter(string -> string.startsWith(args[0])).collect(Collectors.toList());
        }
        list.remove("help");
        list.remove("reload");
        if (list.contains(args[0])) {
            return getServer().getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        }
        return null;
    }

    public boolean hasPermissionM(CommandSender sender, Permission permission){
        if (sender.hasPermission(permission)){
            return true;
        }else{
            sender.sendMessage("§c你没有权限呢~");
            return false;
        }
    }

    public boolean isOnline(CommandSender sender,Player player){
        if (player != null){
            if (player.isOnline()) {
                return true;
            }
        }
        sender.sendMessage("§c玩家不存在或者不在线");
        return false;
    }

    public void executeCmd(CommandSender sender,String identity,String cmd){
        switch (identity) {
            case "console":
            {
                getServer().dispatchCommand(getServer().getConsoleSender(),cmd);
                break;
            }
            case "player":
            {
                getServer().dispatchCommand(sender,cmd);
                break;
            }
            case "op":
            {
                Bukkit.getScheduler().runTask(this,()->{
                    Player player = (Player) sender;
                    player.setOp(true);
                    getServer().dispatchCommand(player,cmd);
                    player.setOp(false);
                });
                break;
            }
        }
    }
}
