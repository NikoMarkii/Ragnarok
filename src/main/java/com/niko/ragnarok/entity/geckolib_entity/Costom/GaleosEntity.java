         @Override
         public void tick() {
             tickScheduledBlockWaves();
             tickWaveBlockDamage();

             LivingEntity t = mob.getTarget();
             if (t == null || !t.isAlive()) {
                 mob.setAttackState(0);
                 return;
             }
             this.target = t;

             // ---- 攻撃中 ----
             if (mob.getAttackState() > 0) {
                 mob.getNavigation().stop();          // 移動停止
                 faceTarget(t);                       // 攻撃方向に体を向ける
                 attackTimer++;
                 executeAttack(t);
                 return;
             }

             // ---- クールダウン中でも移動と向き更新 ----
             if (cooldown > 0) {
                 cooldown--;
                 mob.getLookControl().setLookAt(t, 30F, 30F);  // ターゲットを見続ける
                 mob.getNavigation().moveTo(t, this.speed);    // 移動を継続
                 return;
             }

             // ---- 攻撃開始（attackState == 0 && cooldown == 0） ----
             startAttack();
         }
