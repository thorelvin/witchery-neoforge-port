package com.emoniph.witchery.blocks.entity;

import com.emoniph.witchery.blocks.PlacedModonomiconBlock;
import com.emoniph.witchery.book.PlacedModonomiconBookResolver;
import com.emoniph.witchery.init.WitcheryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PlacedModonomiconBlockEntity extends BlockEntity {
    private static final String BOOK_KEY = "Book";
    private static final String OPEN_PAGES_INDEX_KEY = "OpenPagesIndex";
    private static final double ACTIVE_RANGE = 5.0D;
    private static final double ACTIVE_RANGE_SQR = ACTIVE_RANGE * ACTIVE_RANGE;
    private static final int READER_GRACE_TICKS = 12;
    private static final int OPENING_PAGE_TURNS = 3;

    private ItemStack bookStack = ItemStack.EMPTY;
    private int openPagesIndex;
    private int readerGraceTicks;

    public float bookYaw;
    public float bookYawIncrement;
    public float degreesSpun;
    public float prevDegreesSpun;
    public float openedPercent = 1.0F;
    public float prevOpenedPercent = 1.0F;
    public float degreesOpened = 90.0F;
    public float floppedPercent = 1.0F;
    public float prevFloppedPercent = 1.0F;
    public float degreesFlopped = 90.0F;
    public float pageOneRotation;
    public float prevPageOneRotation;
    public float pageOneRotationTo;
    public float pageOneRotationSpeed = 0.01F;
    public float pageTwoRotation;
    public float prevPageTwoRotation;
    public float pageTwoRotationTo;
    public float pageTwoRotationSpeed = 0.01F;
    public int turnPage;
    public int openingPageTurnsRemaining;

    public PlacedModonomiconBlockEntity(BlockPos pos, BlockState state) {
        super(WitcheryBlockEntities.PLACED_MODONOMICON.get(), pos, state);
        resetBookRotations(facingYaw(state));
        this.openingPageTurnsRemaining = 0;
    }

    public ItemStack getBookStack() {
        return bookStack;
    }

    public int getOpenPagesIndex() {
        return sanitizeOpenPagesIndex(openPagesIndex);
    }

    public void setBookStack(ItemStack stack) {
        boolean hadBook = !bookStack.isEmpty();
        this.bookStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        this.openPagesIndex = 0;
        resetBookRotations(facingYaw(getBlockState()));
        this.openingPageTurnsRemaining = this.bookStack.isEmpty() ? 0 : OPENING_PAGE_TURNS;
        setChangedAndSync();
        if (hadBook != !bookStack.isEmpty()) {
            notifySupportingAltar();
        }
    }

    public ItemStack takeBookStack() {
        ItemStack stored = bookStack;
        boolean hadBook = !bookStack.isEmpty();
        bookStack = ItemStack.EMPTY;
        openPagesIndex = 0;
        resetBookRotations(facingYaw(getBlockState()));
        this.openingPageTurnsRemaining = 0;
        setChangedAndSync();
        if (hadBook) {
            notifySupportingAltar();
        }
        return stored;
    }

    public void markReaderInteraction() {
        this.readerGraceTicks = READER_GRACE_TICKS;
    }

    public boolean markReaderInteractionAndCanTurnPage() {
        if (level != null && level.isClientSide()) {
            boolean canTurnPage = isOpenAndIdleForPageInteraction();
            markReaderInteraction();
            return canTurnPage;
        }

        return false;
    }

    public boolean turnPlacedPage(int direction) {
        return turnPlacedPage(direction, null);
    }

    public boolean turnPlacedPage(int direction, @Nullable Player reader) {
        boolean clientSide = level != null && level.isClientSide();
        if (bookStack.isEmpty() || direction == 0 || (clientSide && turnPage != 0)) {
            return false;
        }

        int pageCount = PlacedModonomiconBookResolver.getPageCount(bookStack, level, reader);
        if (pageCount <= 0) {
            return false;
        }

        int current = PlacedModonomiconBookResolver.clampOpenPagesIndex(bookStack, level, reader, openPagesIndex);
        int next = nextOpenPagesIndex(current, pageCount, direction);
        if (next == current) {
            openPagesIndex = current;
            return false;
        }

        openPagesIndex = next;
        if (clientSide) {
            startPageTurn(direction > 0 ? 1 : 2);
        } else if (level != null) {
            turnPage = 0;
            setChangedAndSync();
        }
        return true;
    }

    public boolean jumpPlacedPage(int targetOpenPagesIndex, @Nullable Player reader) {
        boolean clientSide = level != null && level.isClientSide();
        if (bookStack.isEmpty() || targetOpenPagesIndex < 0 || (clientSide && turnPage != 0)) {
            return false;
        }

        int pageCount = PlacedModonomiconBookResolver.getPageCount(bookStack, level, reader);
        if (pageCount <= 0) {
            return false;
        }

        int current = PlacedModonomiconBookResolver.clampOpenPagesIndex(bookStack, level, reader, openPagesIndex);
        int next = PlacedModonomiconBookResolver.clampOpenPagesIndex(bookStack, level, reader, targetOpenPagesIndex);
        if (next == current) {
            openPagesIndex = current;
            return false;
        }

        openPagesIndex = next;
        if (clientSide) {
            startPageTurn(next > current ? 1 : 2);
        } else if (level != null) {
            turnPage = 0;
            setChangedAndSync();
        }
        return true;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput tag) {
        super.saveAdditional(tag);
        if (!bookStack.isEmpty()) {
            tag.store(BOOK_KEY, ItemStack.CODEC, bookStack);
        }
        tag.putInt(OPEN_PAGES_INDEX_KEY, openPagesIndex);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput tag) {
        super.loadAdditional(tag);
        boolean hadBook = !bookStack.isEmpty();
        int previousOpenPagesIndex = openPagesIndex;
        bookStack = tag.read(BOOK_KEY, ItemStack.CODEC).orElse(ItemStack.EMPTY);
        openPagesIndex = sanitizeOpenPagesIndex(tag.getIntOr(OPEN_PAGES_INDEX_KEY, 0));
        if (hadBook != !bookStack.isEmpty()) {
            resetBookRotations(facingYaw(getBlockState()));
            this.openingPageTurnsRemaining = this.bookStack.isEmpty() ? 0 : OPENING_PAGE_TURNS;
        } else if (level != null && level.isClientSide()
                && !bookStack.isEmpty()
                && previousOpenPagesIndex != openPagesIndex
                && turnPage == 0) {
            startPageTurn(openPagesIndex > previousOpenPagesIndex ? 1 : 2);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, PlacedModonomiconBlockEntity be) {
        be.prevOpenedPercent = be.openedPercent;
        be.prevFloppedPercent = be.floppedPercent;
        be.prevDegreesSpun = be.degreesSpun;
        be.prevPageOneRotation = be.pageOneRotation;
        be.prevPageTwoRotation = be.pageTwoRotation;

        if (be.bookStack.isEmpty()) {
            be.resetBookRotations(facingYaw(state));
            be.openingPageTurnsRemaining = 0;
            return;
        }

        Player nearest = level.getNearestPlayer(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                ACTIVE_RANGE,
                false);

        boolean hasReader = false;
        if (nearest == null) {
            be.readerGraceTicks = 0;
        } else if (isNearbyReader(pos, nearest)) {
            be.readerGraceTicks = READER_GRACE_TICKS;
            hasReader = true;
        } else if (be.readerGraceTicks > 0) {
            be.readerGraceTicks--;
            hasReader = true;
        }

        if (hasReader && nearest != null) {
            be.openAndTargetPlayerLikeHexerei(pos, nearest);
        } else {
            be.closeLikeHexerei();
        }

        be.tickPageFlip(hasReader);
    }

    private void openAndTargetPlayerLikeHexerei(BlockPos pos, Player player) {
        if (this.floppedPercent < 0.9F) {
            float targetYaw = targetYawToPlayer(pos, player);
            this.bookYawIncrement = updateIncrement(this.bookYaw, targetYaw, this.bookYawIncrement);
            this.bookYaw = updateAngle(this.bookYaw, this.bookYawIncrement);
        }

        float degreesFloppedSpeed = (3.0F + 6.0F * Math.abs(this.floppedPercent - 0.66F)) / 90.0F / 2.0F;
        float degreesOpenedSpeed = (2.0F + 5.0F * (0.5F - Math.abs(0.5F - this.openedPercent))) / 90.0F / 2.0F;
        this.floppedPercent = moveTo(this.floppedPercent, 0.0F, degreesFloppedSpeed);
        this.openedPercent = moveTo(this.openedPercent, 0.0F, degreesOpenedSpeed);
        updateDegreeFields();
    }

    private void closeLikeHexerei() {
        float degreesOpenedSpeed = (2.0F + 6.0F * (0.5F - Math.abs(0.5F - this.openedPercent))) / 90.0F / 2.0F;
        float degreesFloppedSpeed = (2.0F + 4.0F * (0.5F - Math.abs(0.5F - this.floppedPercent))) / 90.0F / 2.0F;
        this.openedPercent = moveTo(this.openedPercent, 1.0F, degreesOpenedSpeed);
        this.floppedPercent = moveTo(this.floppedPercent, 1.0F, degreesFloppedSpeed);
        this.turnPage = 0;
        this.pageOneRotation = 0.0F;
        this.pageOneRotationTo = 0.0F;
        this.pageTwoRotation = 0.0F;
        this.pageTwoRotationTo = 0.0F;
        updateDegreeFields();
    }

    private void tickPageFlip(boolean hasPlayer) {
        if (!hasPlayer || this.openedPercent > 0.12F || this.floppedPercent > 0.12F) {
            if (this.turnPage == 0) {
                resetPageTurnRotations();
            }
            return;
        }

        if (this.turnPage == 0 && this.openingPageTurnsRemaining > 0) {
            this.turnPage = 1;
        }

        if (this.turnPage == 1) {
            if (this.pageOneRotation >= 180.0F) {
                resetPageOneTurn();
                this.turnPage = 0;
                if (this.openingPageTurnsRemaining > 0) {
                    this.openingPageTurnsRemaining--;
                }
            } else {
                float sine = Mth.sin(this.pageOneRotation / 180.0F * Mth.PI);
                this.pageOneRotationSpeed = sine * sine * 35.0F + 10.0F;
                this.pageOneRotationTo = 180.0F;
            }
        } else if (this.turnPage == 2) {
            if (this.pageTwoRotation >= 180.0F) {
                resetPageTwoTurn();
                this.turnPage = 0;
            } else {
                float sine = Mth.sin(this.pageTwoRotation / 180.0F * Mth.PI);
                this.pageTwoRotationSpeed = sine * sine * 35.0F + 10.0F;
                this.pageTwoRotationTo = 180.0F;
            }
        }

        this.pageOneRotation = moveTo(this.pageOneRotation, this.pageOneRotationTo, this.pageOneRotationSpeed);
        this.pageTwoRotation = moveTo(this.pageTwoRotation, this.pageTwoRotationTo, this.pageTwoRotationSpeed);
    }

    private void startPageTurn(int turnPage) {
        this.turnPage = turnPage;
        if (turnPage == 1) {
            resetPageOneTurn();
        } else if (turnPage == 2) {
            resetPageTwoTurn();
        }
    }

    private void resetPageTurnRotations() {
        resetPageOneTurn();
        resetPageTwoTurn();
    }

    private void resetPageOneTurn() {
        this.pageOneRotation = 0.0F;
        this.prevPageOneRotation = 0.0F;
        this.pageOneRotationTo = 0.0F;
        this.pageOneRotationSpeed = 0.01F;
    }

    private void resetPageTwoTurn() {
        this.pageTwoRotation = 0.0F;
        this.prevPageTwoRotation = 0.0F;
        this.pageTwoRotationTo = 0.0F;
        this.pageTwoRotationSpeed = 0.01F;
    }

    private void updateDegreeFields() {
        this.degreesSpun = this.bookYaw;
        this.degreesFlopped = this.floppedPercent * 90.0F;
        this.degreesOpened = easeOpened(this.openedPercent) * 90.0F;
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void notifySupportingAltar() {
        if (level != null
                && !level.isClientSide()
                && level.getBlockEntity(worldPosition.below()) instanceof AltarBlockEntity altar) {
            altar.invalidate(level);
        }
    }

    private void resetBookRotations(float yaw) {
        this.bookYaw = yaw;
        this.bookYawIncrement = 0.0F;
        this.degreesSpun = yaw;
        this.prevDegreesSpun = yaw;
        this.openedPercent = 1.0F;
        this.prevOpenedPercent = 1.0F;
        this.degreesOpened = 90.0F;
        this.floppedPercent = 1.0F;
        this.prevFloppedPercent = 1.0F;
        this.degreesFlopped = 90.0F;
        this.pageOneRotation = 0.0F;
        this.prevPageOneRotation = 0.0F;
        this.pageOneRotationTo = 0.0F;
        this.pageOneRotationSpeed = 0.01F;
        this.pageTwoRotation = 0.0F;
        this.prevPageTwoRotation = 0.0F;
        this.pageTwoRotationTo = 0.0F;
        this.pageTwoRotationSpeed = 0.01F;
        this.turnPage = 0;
        this.readerGraceTicks = 0;
    }

    private boolean isOpenAndIdleForPageInteraction() {
        return this.openedPercent <= 0.12F
                && this.floppedPercent <= 0.12F
                && this.openingPageTurnsRemaining <= 0
                && this.turnPage == 0;
    }

    private static float targetYawToPlayer(BlockPos pos, Player player) {
        float angle = (float) Math.toDegrees(Math.atan2(
                player.getZ() - pos.getZ() - 0.5D,
                player.getX() - pos.getX() - 0.5D));
        if (angle < 0.0F) {
            angle += 360.0F;
        }
        return 270.0F - angle;
    }

    private static boolean isNearbyReader(BlockPos pos, Player player) {
        return isNearbyReader(pos, player.getX(), player.getEyeY(), player.getZ());
    }

    static boolean isNearbyReader(BlockPos pos, double playerX, double playerEyeY, double playerZ) {
        double bookX = pos.getX() + 0.5D;
        double bookY = pos.getY() + 0.35D;
        double bookZ = pos.getZ() + 0.5D;
        double dx = bookX - playerX;
        double dy = bookY - playerEyeY;
        double dz = bookZ - playerZ;
        return dx * dx + dy * dy + dz * dz <= ACTIVE_RANGE_SQR
                && playerEyeY + 0.25D >= bookY;
    }

    public static float easeFlop(float x) {
        return x < 0.5F
                ? (float) Math.pow(2.0D, 20.0F * x - 10.0F) / 2.0F
                : (float) (2.0D - Math.pow(2.0D, -20.0F * x + 10.0F)) / 2.0F;
    }

    public static float easeOpened(float x) {
        float c1 = 1.0F;
        float c2 = c1 * 1.525F;
        return x < 0.5F
                ? (float) (Math.pow(2.0F * x, 2.0D) * ((c2 + 1.0F) * 2.0F * x - c2)) / 2.0F
                : (float) (2.0D - Math.pow(2.0D, -20.0F * x + 10.0F)) / 2.0F;
    }

    private static float updateIncrement(float currentAngle, float targetAngle, float lastIncrement) {
        targetAngle = normalizeAngle(targetAngle);
        currentAngle = normalizeAngle(currentAngle);
        float angleDifference = targetAngle - currentAngle;
        if (angleDifference > 180.0F) {
            angleDifference -= 360.0F;
        } else if (angleDifference < -180.0F) {
            angleDifference += 360.0F;
        }

        float distance = Math.abs(angleDifference);
        if (Mth.abs(lastIncrement) < 0.1F && distance < 0.9F) {
            return 0.0F;
        }

        float adjustment = (distance / 180.0F * (distance / 180.0F) + 0.175F) * (angleDifference > 0.0F ? 1.0F : -1.0F);
        return Mth.abs(lastIncrement) < 0.8F && distance < 10.0F
                ? (lastIncrement + adjustment) * (0.72F + 0.2F * Mth.abs(lastIncrement) / 0.8F)
                : (lastIncrement + adjustment) * 0.92F;
    }

    private static float updateAngle(float currentAngle, float maxIncrement) {
        return normalizeAngle(currentAngle + maxIncrement);
    }

    private static float normalizeAngle(float angle) {
        while (angle > 90.0F) {
            angle -= 360.0F;
        }
        while (angle < -270.0F) {
            angle += 360.0F;
        }
        return angle;
    }

    private static int sanitizeOpenPagesIndex(int openPagesIndex) {
        return Math.max(0, openPagesIndex) & -2;
    }

    static int nextOpenPagesIndex(int openPagesIndex, int pageCount, int direction) {
        if (pageCount <= 0) {
            return 0;
        }

        int current = sanitizeOpenPagesIndex(openPagesIndex);
        if (current >= pageCount) {
            current = (pageCount - 1) & -2;
        }
        current = Math.max(0, current);

        if (direction > 0) {
            return Math.min(current + 2, (pageCount - 1) & -2);
        }
        if (direction < 0) {
            return Math.max(current - 2, 0);
        }
        return current;
    }

    private static float moveTo(float current, float target, float step) {
        if (current < target) {
            return Math.min(current + step, target);
        }
        return Math.max(current - step, target);
    }

    private static float facingYaw(BlockState state) {
        Direction facing = state.hasProperty(PlacedModonomiconBlock.FACING)
                ? state.getValue(PlacedModonomiconBlock.FACING)
                : Direction.SOUTH;
        return switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            case NORTH -> 180.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
    }
}
