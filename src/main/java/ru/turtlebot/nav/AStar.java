package ru.turtlebot.nav;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.turtlebot.config.BotConfig;

import java.util.*;
import java.util.function.Predicate;

/** Собственный A*: только ходьба по ровной поверхности, спуск до maxDrop, без прыжков и подъёмов. */
public final class AStar {
    private static final int[][] DIRS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    private static final double EYE = 1.62;

    private static final class Node {
        final BlockPos pos; final Node parent; final double g, f; boolean closed;
        Node(BlockPos pos, Node parent, double g, double f) {
            this.pos = pos; this.parent = parent; this.g = g; this.f = f;
        }
    }

    private AStar() {}

    public static List<BlockPos> search(BlockPos start, Vec3d startEye, Predicate<Vec3d> eyeOk,
                                        Vec3d target, double reach, Walkability w, BotConfig cfg) {
        w.autoStrict(start);
        if (eyeOk.test(startEye)) return new ArrayList<>();

        Map<Long, Node> all = new HashMap<>();
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Node s = new Node(start, null, 0, h(start, target, reach));
        all.put(start.asLong(), s);
        open.add(s);
        int expanded = 0;

        while (!open.isEmpty() && expanded < cfg.maxPathNodes) {
            Node cur = open.poll();
            if (cur.closed || all.get(cur.pos.asLong()) != cur) continue;
            cur.closed = true;
            expanded++;
            if (cur != s && eyeOk.test(eye(cur.pos))) return build(cur);

            int cx = cur.pos.getX(), cy = cur.pos.getY(), cz = cur.pos.getZ();
            for (int[] d : DIRS) {
                int nx = cx + d[0], nz = cz + d[1];
                if (Math.abs(nx - start.getX()) > cfg.maxPathRange
                        || Math.abs(nz - start.getZ()) > cfg.maxPathRange) continue;
                boolean diag = d[0] != 0 && d[1] != 0;
                if (diag && (!w.diagOk(new BlockPos(cx + d[0], cy, cz))
                        || !w.diagOk(new BlockPos(cx, cy, cz + d[1])))) continue;
                BlockPos n0 = new BlockPos(nx, cy, nz);
                if (!w.bodyClear(n0)) continue;
                for (int dd = 0; dd <= cfg.maxDrop; dd++) {
                    BlockPos n = n0.down(dd);
                    if (dd > 0 && !w.passable(n)) break;
                    if (w.standable(n)) {
                        double g2 = cur.g + (diag ? 1.4142 : 1.0) + dd * 0.5;
                        Node old = all.get(n.asLong());
                        if (old == null || g2 < old.g) {
                            Node nn = new Node(n, cur, g2, g2 + h(n, target, reach));
                            all.put(n.asLong(), nn);
                            open.add(nn);
                        }
                        break;
                    }
                }
            }
        }
        return null;
    }

    private static Vec3d eye(BlockPos p) {
        return new Vec3d(p.getX() + 0.5, p.getY() + EYE, p.getZ() + 0.5);
    }

    private static double h(BlockPos p, Vec3d target, double reach) {
        return Math.max(0, eye(p).distanceTo(target) - reach);
    }

    private static List<BlockPos> build(Node end) {
        LinkedList<BlockPos> out = new LinkedList<>();
        for (Node n = end; n != null && n.parent != null; n = n.parent) out.addFirst(n.pos);
        return new ArrayList<>(out);
    }
}
