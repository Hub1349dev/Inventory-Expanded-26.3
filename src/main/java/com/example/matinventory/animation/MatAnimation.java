package com.example.matinventory.animation;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class MatAnimation {
   private final long opened = System.nanoTime();
   private long previous = this.opened;
   private final float[] hover = new float[512];
   public float elapsed;
   public float dt;
   public static final float UNFOLD_SECONDS = 0.59090906F;
   public static final float DROP_SECONDS = 0.1F;
   public static final float DROP_HEIGHT = 0.09F;
   public static final float ITEMS_SECONDS = 1.2F;
   private boolean opening;
   private boolean prepared;
   private boolean revealed;
   private boolean closing;
   private float closeAt;
   private float closeItems;
   private float closeMotion;
   private float closeArrival;
   private float motionAt = Float.POSITIVE_INFINITY;
   private float revealAt = Float.POSITIVE_INFINITY;
   private float lastDelay;
   private float itemLeft = -14.0F;
   private float itemRight = 18.0F;

   public void startOpening() {
      this.opening = true;
   }

   public long cycleId() {
      return this.opened;
   }

   public boolean closing() {
      return this.closing;
   }

   public void startClosing(boolean populated) {
      if (!this.closing) {
         this.closeMotion = Math.clamp(this.openingClock(), 0.0F, 1.02F);
         float itemWindow = this.lastDelay > 0.0F ? this.lastDelay : (this.itemRight - this.itemLeft > 0.001F ? 1.2F : 0.1F);
         this.closeArrival = populated && this.revealed ? Math.clamp(this.arrivalClock(), 0.0F, itemWindow) : 0.0F;
         this.closeItems = Math.min(0.1F, this.closeArrival);
         this.closeAt = this.elapsed;
         this.closing = true;
      }
   }

   public float closingElapsed() {
      return this.closing ? Math.max(0.0F, this.elapsed - this.closeAt) : 0.0F;
   }

   public float closeItemSeconds() {
      return this.closeItems;
   }

   public float closeMotionSeconds() {
      return this.closeMotion;
   }

   public boolean closingMat() {
      return this.closing && this.closingElapsed() >= this.closeItems;
   }

   public float closeMotionRemaining() {
      return this.closing ? Math.max(0.0F, this.closeMotion - Math.max(0.0F, this.closingElapsed() - this.closeItems)) : 0.0F;
   }

   public boolean closeFinished() {
      return this.closing && this.closingElapsed() >= this.closeItems + this.closeMotion;
   }

   public void cancelClosing() {
      if (this.closing) {
         this.closing = false;
         this.prepared = this.revealed = true;
         this.motionAt = this.elapsed - 1.02F;
         this.revealAt = this.elapsed - 1.2F;
      }
   }

   public void itemRange(float left, float right) {
      this.itemLeft = left;
      this.itemRight = right;
   }

   private float delay(float x) {
      float width = this.itemRight - this.itemLeft;
      return width <= 0.001F ? 0.0F : Math.clamp((x - this.itemLeft) / width, 0.0F, 1.0F) * 1.1F;
   }

   public void itemsPrepared(boolean populated) {
      if (!this.prepared) {
         this.prepared = true;
         this.motionAt = this.elapsed;
         this.lastDelay = populated ? this.delay(this.itemRight) + 0.1F : 0.0F;
      }
   }

   private float arrivalClock() {
      return this.elapsed - this.revealAt;
   }

   private float reversePhase(float x) {
      return Math.clamp(this.closeArrival - this.delay(x), 0.0F, 0.1F) - this.closingElapsed();
   }

   public void revealWhenReady() {
      if (!this.closing && this.opening && this.prepared && !this.revealed && this.matOpen() == 1.0F) {
         this.revealed = true;
         this.revealAt = this.elapsed;
      }
   }

   public boolean itemsVisible() {
      return this.closing ? this.revealed && this.closingElapsed() < this.closeItems : !this.opening || this.revealed;
   }

   public boolean itemVisible(float x) {
      return this.itemsVisible()
         && (this.closing ? this.closeArrival >= this.delay(x) && this.reversePhase(x) >= 0.0F : !this.opening || this.arrivalClock() >= this.delay(x));
   }

   public boolean arrivalsSettled() {
      return !this.opening || this.revealed && this.arrivalClock() >= this.lastDelay;
   }

   public boolean itemsSettled() {
      return !this.closing && this.arrivalsSettled();
   }

   public float openingEndClock() {
      return Math.max(1.02F, 0.68090904F + this.lastDelay);
   }

   public float openingClock() {
      return this.closing ? this.closeMotionRemaining() : (!this.opening ? Float.POSITIVE_INFINITY : (this.prepared ? this.elapsed - this.motionAt : -1.0F));
   }

   public boolean cameraOpening() {
      return this.closing || this.opening && this.openingClock() < 1.02F;
   }

   public float matFall() {
      return this.opening ? OpeningMotion.fall(Math.max(0.0F, this.openingClock())) : 0.0F;
   }

   public float matOpen() {
      return this.opening ? (this.prepared ? OpeningMotion.fold(this.openingClock()) : 0.0F) : 1.0F;
   }

   public void frame() {
      long now = System.nanoTime();
      this.dt = Math.min((float)(now - this.previous) / 1.0E9F, 0.05F);
      this.elapsed = (float)(now - this.opened) / 1.0E9F;
      this.previous = now;
   }

   public float lift(int index, boolean active) {
      float target = active ? 1.0F : 0.0F;
      this.hover[index] = this.hover[index] + (target - this.hover[index]) * (1.0F - (float)Math.exp(-this.dt * 16.0F));
      if (Math.abs(target - this.hover[index]) < 0.001F) {
         this.hover[index] = target;
      }

      return this.hover[index];
   }

   public float dropAt(float x) {
      if (this.opening && this.itemsVisible()) {
         float t = Math.clamp((this.closing ? this.reversePhase(x) : this.arrivalClock() - this.delay(x)) / 0.1F, 0.0F, 1.0F);
         return (1.0F - t) * (1.0F - t) * 0.09F;
      } else {
         return 0.0F;
      }
   }
}
