package dev.flower.manualriding;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;

import java.util.List;
import java.util.function.BooleanSupplier;

/** 暂停原生 AI 目标并在结束时恢复原对象。 / Suspend native goals and restore their original instances. */
public final class ManualSession {
    private final Mob mob;
    private final List<WrappedGoal> movementSnapshot;
    private final List<WrappedGoal> targetSnapshot;
    private final GoalSelector goalSelector;
    private final GoalSelector targetSelector;

    private Goal active;
    private BooleanSupplier continuation;
    private boolean closed;

    public ManualSession(Mob mob) {
        this.mob = mob;
        this.goalSelector = mob.goalSelector;
        this.targetSelector = mob.targetSelector;
        this.movementSnapshot = List.copyOf(goalSelector.getAvailableGoals());
        this.targetSnapshot = List.copyOf(targetSelector.getAvailableGoals());

        for (WrappedGoal goal : movementSnapshot) {
            goalSelector.removeGoal(goal.getGoal());
        }

        for (WrappedGoal goal : targetSnapshot) {
            targetSelector.removeGoal(goal.getGoal());
        }

        mob.setTarget(null);
        mob.getNavigation().stop();
    }

    public List<WrappedGoal> goals() {
        return movementSnapshot;
    }

    public boolean busy() {
        return active != null;
    }

    public boolean run(Goal goal, BooleanSupplier canContinue) {
        if (closed || active != null) {
            return false;
        }

        goal.start();
        active = goal;
        continuation = canContinue != null ? canContinue : goal::canContinueToUse;
        return true;
    }

    public Goal tick() {
        if (closed || active == null) {
            return null;
        }

        if (continuation.getAsBoolean()) {
            active.tick();
            return null;
        }

        Goal finished = active;
        active = null;
        continuation = null;
        finished.stop();
        return finished;
    }

    public void close() {
        if (closed) {
            return;
        }

        if (active != null) {
            active.stop();
            active = null;
            continuation = null;
        }

        for (WrappedGoal goal : movementSnapshot) {
            goalSelector.addGoal(goal.getPriority(), goal.getGoal());
        }

        for (WrappedGoal goal : targetSnapshot) {
            targetSelector.addGoal(goal.getPriority(), goal.getGoal());
        }

        mob.setTarget(null);
        mob.getNavigation().stop();
        closed = true;
    }
}
