package dev.kizuna.core.impl;

import dev.kizuna.core.Manager;
import dev.kizuna.Kawaii;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EnemyManager extends Manager {
    public EnemyManager() {
        read();
    }
    public final ArrayList<String> enemyList = new ArrayList<>();
    public boolean isEnemy(String name) {
        return enemyList.contains(name);
    }
    public void removeEnemy(String name) {
        enemyList.remove(name);
    }
    public void addEnemy(String name) {
        if (!enemyList.contains(name)) {
            enemyList.add(name);
        }
    }

    public void enemy(PlayerEntity entity) {
        enemy(entity.getGameProfile().getName());
    }

    public void enemy(String name) {
        if (enemyList.contains(name)) {
            enemyList.remove(name);
        } else {
            enemyList.add(name);
        }
    }

    public void read() {
        try {
            File ememyFile = getFile("enemies.txt");
            if (!ememyFile.exists())
                return;
            List<String> list = IOUtils.readLines(new FileInputStream(ememyFile), StandardCharsets.UTF_8);

            for (String s : list) {
                addEnemy(s);
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void save() {
        try {
            File enemyFile = getFile("enemies.txt");
            PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(enemyFile), StandardCharsets.UTF_8));
            for (String str : enemyList) {
                printwriter.println(str);
            }
            printwriter.close();
        } catch (Exception exception) {
            System.out.println("[" + Kawaii.NAME + "] Failed to save Enemies");
        }
    }


    public boolean isEnemy(PlayerEntity entity) {
        return isEnemy(entity.getGameProfile().getName());
    }
}
