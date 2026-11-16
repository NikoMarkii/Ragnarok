package com.niko.ragnarok.entity.geckolib_entity.Costom.Boss.VolcanicSerpent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class VolcanicSerpentBodyEntity {
    private static final int MAX_TRAIL_SAMPLES = 2048;
    private static final double MIN_TRAIL_DISTANCE = 0.12D;
    private static final float MAX_ROLL_GAP = 18.0F;
    private static final float ROLL_FOLLOW = 0.22F;

    private final VolcanicSerpentHeadEntity head;
    private final VolcanicSerpentPartEntity[] parts;
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();

    // クライアント補間用キャッシュ
    private Vec3[] clientPrevPositions;
    private Vec3[] clientCurrPositions;
    private float[] clientPrevYaws;
    private float[] clientCurrYaws;
    private float[] clientPrevPitches;
    private float[] clientCurrPitches;
    private float[] clientPrevRolls;
    private float[] clientCurrRolls;
    private boolean hasSnapshot = false;

    public VolcanicSerpentBodyEntity(VolcanicSerpentHeadEntity head, int segmentCount) {
        this.head = head;
        this.parts = new VolcanicSerpentPartEntity[segmentCount];
        for (int i = 0; i < segmentCount; i++) {
            boolean isTail = (i == segmentCount - 1);
            this.parts[i] = new VolcanicSerpentPartEntity(head, i + 1, isTail);
        }
        this.clientPrevPositions = new Vec3[segmentCount];
        this.clientCurrPositions = new Vec3[segmentCount];
        this.clientPrevYaws = new float[segmentCount];
        this.clientCurrYaws = new float[segmentCount];
        this.clientPrevPitches = new float[segmentCount];
        this.clientCurrPitches = new float[segmentCount];
        this.clientPrevRolls = new float[segmentCount];
        this.clientCurrRolls = new float[segmentCount];
    }

    public VolcanicSerpentPartEntity[] getParts() {
        return parts;
    }

    public Entity getSection(int index) {
        return index == 0 ? head : parts[index - 1];
    }

    public float getRoll(int index) {
        return index == 0 ? head.getRoll(1.0F) : parts[index - 1].getRoll();
    }

    public void assignIds(int headId) {
        for (int i = 0; i < parts.length; i++) {
            parts[i].setId(headId + i + 1);
        }
    }

    public void remove() {
        for (VolcanicSerpentPartEntity part : parts) {
            part.setRemoved(Entity.RemovalReason.DISCARDED);
        }
    }

    public void initializePosition(double segmentSpacing) {
        if (!trail.isEmpty() || hasSnapshot) return;
        Vec3 forward = head.getLookAngle();
        if (forward.lengthSqr() < 1.0E-4) forward = new Vec3(0, 0, 1);
        for (VolcanicSerpentPartEntity part : parts) {
            part.updatePose(head.position().subtract(forward.scale(part.getIndex() * segmentSpacing)), forward, 0.0F);
        }
    }

    public void initializeTrail(double segmentSpacing) {
        if (!trail.isEmpty()) return;
        Vec3 look = head.getLookAngle();
        if (look.lengthSqr() < 1.0E-4) look = new Vec3(0, 0, 1);
        Vec3 backward = look.normalize().scale(-segmentSpacing);
        for (int i = 0; i <= parts.length + 2; i++) {
            trail.addLast(head.position().add(backward.scale(i)));
        }
    }

    public void update(double segmentSpacing) {
        initializeTrail(segmentSpacing);

        Vec3 headPos = head.position();
        if (trail.getFirst().distanceToSqr(headPos) >= MIN_TRAIL_DISTANCE * MIN_TRAIL_DISTANCE) {
            trail.addFirst(headPos);
        }
        while (trail.size() > MAX_TRAIL_SAMPLES) {
            trail.removeLast();
        }

        // 先頭を現在の頭の位置に固定したパスリストを作成し、常に一定間隔を保つ
        List<Vec3> path = new ArrayList<>(trail.size() + 1);
        path.add(headPos);
        path.addAll(trail);

        int cursor = 0;
        double traversed = 0.0D;

        for (VolcanicSerpentPartEntity part : parts) {
            int index = part.getIndex();
            double targetDistance = index * segmentSpacing;

            while (cursor + 1 < path.size()) {
                double segLen = path.get(cursor).distanceTo(path.get(cursor + 1));
                if (traversed + segLen >= targetDistance) break;
                traversed += segLen;
                cursor++;
            }

            Vec3 from = path.get(cursor);
            Vec3 to = path.get(Math.min(cursor + 1, path.size() - 1));
            double length = from.distanceTo(to);
            double t = length < 1.0E-5 ? 0.0D : Mth.clamp((targetDistance - traversed) / length, 0.0D, 1.0D);
            Vec3 nextPos = from.lerp(to, t);

            // 1つ前の節の方向を向くことで自然な連結角度を維持
            Vec3 prevSectionPos = getSection(index - 1).position();
            Vec3 forward = prevSectionPos.subtract(nextPos);
            if (forward.lengthSqr() < 1.0E-6) {
                forward = head.getLookAngle();
            }

            // ロール（ねじれ）を前の節から遅延追従させる
            float prevRoll = getRoll(index - 1);
            float nextRoll = followRoll(prevRoll, part.getRoll());

            part.updatePose(nextPos, forward, nextRoll);
        }

        while (trail.size() > cursor + 4) {
            trail.removeLast();
        }
    }

    private static float followRoll(float previous, float current) {
        float gap = Mth.wrapDegrees(previous - current);
        float step = Math.abs(gap) >= MAX_ROLL_GAP
                ? gap - Math.copySign(MAX_ROLL_GAP - 0.01F, gap)
                : gap * ROLL_FOLLOW;
        return Mth.wrapDegrees(current + step);
    }

    // --- サーバー・クライアント間の全パーツ同期（デストロイヤー方式の軽量バイナリスナップショット） ---

    public CompoundTag createSnapshot() {
        int bytesPerPart = 3 * Float.BYTES + 3 * Float.BYTES; // relX, relY, relZ, yaw, pitch, roll
        ByteBuffer buffer = ByteBuffer.allocate(parts.length * bytesPerPart);
        double hx = head.getX();
        double hy = head.getY();
        double hz = head.getZ();

        for (VolcanicSerpentPartEntity part : parts) {
            buffer.putFloat((float) (part.getX() - hx));
            buffer.putFloat((float) (part.getY() - hy));
            buffer.putFloat((float) (part.getZ() - hz));
            buffer.putFloat(part.getYRot());
            buffer.putFloat(part.getXRot());
            buffer.putFloat(part.getRoll());
        }
        CompoundTag tag = new CompoundTag();
        tag.putByteArray("PartsPose", buffer.array());
        return tag;
    }

    public void readSnapshot(CompoundTag tag) {
        byte[] bytes = tag.getByteArray("PartsPose");
        int bytesPerPart = 6 * Float.BYTES;
        if (bytes.length != parts.length * bytesPerPart) return;

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        double hx = head.getX();
        double hy = head.getY();
        double hz = head.getZ();

        for (int i = 0; i < parts.length; i++) {
            Vec3 pos = new Vec3(hx + buffer.getFloat(), hy + buffer.getFloat(), hz + buffer.getFloat());
            float yaw = buffer.getFloat();
            float pitch = buffer.getFloat();
            float roll = buffer.getFloat();

            if (!hasSnapshot) {
                clientPrevPositions[i] = pos;
                clientPrevYaws[i] = yaw;
                clientPrevPitches[i] = pitch;
                clientPrevRolls[i] = roll;
            } else {
                clientPrevPositions[i] = clientCurrPositions[i];
                clientPrevYaws[i] = clientCurrYaws[i];
                clientPrevPitches[i] = clientCurrPitches[i];
                clientPrevRolls[i] = clientCurrRolls[i];
            }

            clientCurrPositions[i] = pos;
            clientCurrYaws[i] = yaw;
            clientCurrPitches[i] = pitch;
            clientCurrRolls[i] = roll;

            VolcanicSerpentPartEntity part = parts[i];
            part.xo = part.xOld = clientPrevPositions[i].x;
            part.yo = part.yOld = clientPrevPositions[i].y;
            part.zo = part.zOld = clientPrevPositions[i].z;
            part.yRotO = clientPrevYaws[i];
            part.xRotO = clientPrevPitches[i];

            part.setPos(pos);
            part.setYRot(yaw);
            part.setXRot(pitch);
            part.setRoll(roll);
        }
        hasSnapshot = true;
    }

    public void tickClientInterpolation() {
        if (!hasSnapshot) return;
        for (int i = 0; i < parts.length; i++) {
            VolcanicSerpentPartEntity part = parts[i];
            part.xo = part.xOld = part.getX();
            part.yo = part.yOld = part.getY();
            part.zo = part.zOld = part.getZ();
            part.yRotO = part.getYRot();
            part.xRotO = part.getXRot();
        }
    }

    public boolean hasClientSnapshot() {
        return hasSnapshot;
    }

    public Vec3 getRenderPosition(int partIndex, float partialTick) {
        if (hasSnapshot && partIndex >= 0 && partIndex < parts.length) {
            return clientPrevPositions[partIndex].lerp(clientCurrPositions[partIndex], partialTick);
        }
        VolcanicSerpentPartEntity part = parts[partIndex];
        return new Vec3(
                Mth.lerp(partialTick, part.xo, part.getX()),
                Mth.lerp(partialTick, part.yo, part.getY()),
                Mth.lerp(partialTick, part.zo, part.getZ())
        );
    }

    public float getRenderYaw(int partIndex, float partialTick) {
        if (hasSnapshot && partIndex >= 0 && partIndex < parts.length) {
            return Mth.rotLerp(partialTick, clientPrevYaws[partIndex], clientCurrYaws[partIndex]);
        }
        return Mth.rotLerp(partialTick, parts[partIndex].yRotO, parts[partIndex].getYRot());
    }

    public float getRenderPitch(int partIndex, float partialTick) {
        if (hasSnapshot && partIndex >= 0 && partIndex < parts.length) {
            return Mth.rotLerp(partialTick, clientPrevPitches[partIndex], clientCurrPitches[partIndex]);
        }
        return Mth.rotLerp(partialTick, parts[partIndex].xRotO, parts[partIndex].getXRot());
    }

    public float getRenderRoll(int partIndex, float partialTick) {
        if (hasSnapshot && partIndex >= 0 && partIndex < parts.length) {
            return clientPrevRolls[partIndex] + Mth.wrapDegrees(clientCurrRolls[partIndex] - clientPrevRolls[partIndex]) * partialTick;
        }
        return parts[partIndex].getRoll();
    }

    public void save(CompoundTag tag) {
        ListTag history = new ListTag();
        for (Vec3 point : trail) {
            CompoundTag entry = new CompoundTag();
            entry.putDouble("X", point.x);
            entry.putDouble("Y", point.y);
            entry.putDouble("Z", point.z);
            history.add(entry);
        }
        tag.put("SerpentTrail", history);
    }

    public void load(CompoundTag tag, double segmentSpacing) {
        trail.clear();
        ListTag history = tag.getList("SerpentTrail", 10);
        for (int i = 0; i < Math.min(history.size(), MAX_TRAIL_SAMPLES); i++) {
            CompoundTag point = history.getCompound(i);
            trail.addLast(new Vec3(point.getDouble("X"), point.getDouble("Y"), point.getDouble("Z")));
        }
        update(segmentSpacing);
    }
}