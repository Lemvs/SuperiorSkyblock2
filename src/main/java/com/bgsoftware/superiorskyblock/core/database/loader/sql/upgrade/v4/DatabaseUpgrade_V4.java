package com.bgsoftware.superiorskyblock.core.database.loader.sql.upgrade.v4;

import com.bgsoftware.common.databasebridge.sql.query.QueryResult;
import com.bgsoftware.common.databasebridge.sql.transaction.CustomSQLDatabaseTransaction;
import com.bgsoftware.superiorskyblock.core.database.DatabaseResult;
import com.bgsoftware.superiorskyblock.core.database.sql.DBSession;
import com.bgsoftware.superiorskyblock.core.database.sql.ResultSetMapBridge;
import com.bgsoftware.superiorskyblock.core.mutable.MutableBoolean;
import com.bgsoftware.superiorskyblock.island.upgrade.IslandUpgradeConstants;

import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DatabaseUpgrade_V4 implements Runnable {

    public static final DatabaseUpgrade_V4 INSTANCE = new DatabaseUpgrade_V4();

    private DatabaseUpgrade_V4() {

    }

    @Override
    public void run() {
        List<DatabaseItem> itemsToUpdate = new LinkedList<>();

        MutableBoolean isFailed = new MutableBoolean(false);

        DBSession.select("islands_settings", "", new QueryResult<ResultSet>().onSuccess(resultSet -> {
            while (resultSet.next()) {
                DatabaseResult databaseResult = new DatabaseResult(new ResultSetMapBridge(resultSet));

                String island = databaseResult.getString("island").orElse(null);
                if (island == null) {
                    continue;
                }

                Integer size = databaseResult.getInt("size").orElse(null);
                if (size == null) {
                    continue;
                }

                Integer coopsLimit = databaseResult.getInt("coops_limit").orElse(null);
                if (coopsLimit == null) {
                    continue;
                }

                Integer membersLimit = databaseResult.getInt("members_limit").orElse(null);
                if (membersLimit == null) {
                    continue;
                }

                Integer warpsLimit = databaseResult.getInt("warps_limit").orElse(null);
                if (warpsLimit == null) {
                    continue;
                }

                Double cropGrowthMultiplier = databaseResult.getDouble("crop_growth_multiplier").orElse(null);
                if (cropGrowthMultiplier == null) {
                    continue;
                }

                Double spawnerRatesMultiplier = databaseResult.getDouble("spawner_rates_multiplier").orElse(null);
                if (spawnerRatesMultiplier == null) {
                    continue;
                }

                Double mobDropsMultiplier = databaseResult.getDouble("mob_drops_multiplier").orElse(null);
                if (mobDropsMultiplier == null) {
                    continue;
                }

                if (shouldConvert(size, coopsLimit, membersLimit, warpsLimit,
                        cropGrowthMultiplier, spawnerRatesMultiplier, mobDropsMultiplier)) {
                    itemsToUpdate.add(new DatabaseItem(island, size, coopsLimit, membersLimit, warpsLimit,
                            cropGrowthMultiplier, spawnerRatesMultiplier, mobDropsMultiplier));
                }
            }
        }).onFail(error -> isFailed.set(true)));

        if (isFailed.get()) {
            return;
        }

        CustomSQLDatabaseTransaction updateTransaction = new CustomSQLDatabaseTransaction(
                "UPDATE {prefix}islands_settings SET size=?,coops_limit=?,members_limit=?,warps_limit=?," +
                        "crop_growth_multiplier=?,spawner_rates_multiplier=?,mob_drops_multiplier=? WHERE island=?");
        updateManyItems(itemsToUpdate, updateTransaction);
    }

    private static boolean shouldConvert(int size, int coopsLimit, int membersLimit, int warpsLimit,
                                         double cropGrowthMultiplier, double spawnerRatesMultiplier, double mobDropsMultiplier) {
        return size == -1 || coopsLimit == -1 || membersLimit == -1 || warpsLimit == -1
                || cropGrowthMultiplier == -1 || spawnerRatesMultiplier == -1 || mobDropsMultiplier == -1;
    }

    private static void updateManyItems(List<DatabaseItem> items, CustomSQLDatabaseTransaction updateTransaction) {
        for (DatabaseItem item : items) {
            updateTransaction
                    .bindObject(item.size)
                    .bindObject(item.coopsLimit)
                    .bindObject(item.membersLimit)
                    .bindObject(item.warpsLimit)
                    .bindObject(item.cropGrowthMultiplier)
                    .bindObject(item.spawnerRatesMultiplier)
                    .bindObject(item.mobDropsMultiplier)
                    .bindObject(item.island)
                    .newBatch();
        }

        try {
            DBSession.execute(updateTransaction).get();
        } catch (InterruptedException | ExecutionException ignored) {
        }
    }

    private static class DatabaseItem {

        private final String island;
        private final int size;
        private final int coopsLimit;
        private final int membersLimit;
        private final int warpsLimit;
        private final double cropGrowthMultiplier;
        private final double spawnerRatesMultiplier;
        private final double mobDropsMultiplier;

        DatabaseItem(String island, int size, int coopsLimit, int membersLimit, int warpsLimit,
                     double cropGrowthMultiplier, double spawnerRatesMultiplier, double mobDropsMultiplier) {
            this.island = island;
            this.size = size == -1 ? IslandUpgradeConstants.SYNCED_VALUE : size;
            this.coopsLimit = coopsLimit == -1 ? IslandUpgradeConstants.SYNCED_VALUE : coopsLimit;
            this.membersLimit = membersLimit == -1 ? IslandUpgradeConstants.SYNCED_VALUE : membersLimit;
            this.warpsLimit = warpsLimit == -1 ? IslandUpgradeConstants.SYNCED_VALUE : warpsLimit;
            this.cropGrowthMultiplier = cropGrowthMultiplier == -1 ? IslandUpgradeConstants.SYNCED_VALUE : cropGrowthMultiplier;
            this.spawnerRatesMultiplier = spawnerRatesMultiplier == -1 ? IslandUpgradeConstants.SYNCED_VALUE : spawnerRatesMultiplier;
            this.mobDropsMultiplier = mobDropsMultiplier == -1 ? IslandUpgradeConstants.SYNCED_VALUE : mobDropsMultiplier;
        }

    }

}
