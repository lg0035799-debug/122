package kr.murim.skills.data;

import kr.murim.skills.MurimPlugin;

public final class StatService {
    private final MurimPlugin plugin;

    public StatService(MurimPlugin plugin) { this.plugin = plugin; }

    public double maxQi(PlayerData d) {
        return plugin.getConfig().getDouble("qi.base", 50)
                + d.internalPower * plugin.getConfig().getDouble("qi.per-internal-power", 1.0);
    }

    public double qi(PlayerData d) { return Math.min(d.qi, maxQi(d)); }

    public void regen(PlayerData d) {
        d.qi = Math.min(maxQi(d), qi(d) + plugin.getConfig().getDouble("qi.regen-per-second", 2.0));
    }

    public boolean consume(PlayerData d, double cost) {
        double cur = qi(d);
        if (cur < cost) return false;
        d.qi = cur - cost;
        return true;
    }
}
