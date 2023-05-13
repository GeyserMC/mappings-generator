/**
 * Covers block state mapping for standing signs, wall signs, hanging signs, and wall_hanging. On Bedrock edition,
 * there is just standing signs, wall signs, and hanging signs.
 *
 * Standing signs are covered by {@link org.geysermc.generator.state.type.sign.SignRotationMapper} (ground_sign_direction)
 *
 * Wall signs are covered by {@link org.geysermc.generator.state.type.sign.WallSignFacingMapper} (facing_direction)
 *
 * Hanging signs are covered by
 * {@link org.geysermc.generator.state.type.sign.HangingSignAttachedMapper} (attached_bit)
 * {@link org.geysermc.generator.state.type.sign.HangingSignFacingMapper} (facing_direction)
 * {@link org.geysermc.generator.state.type.sign.HangingSignHangingMapper} (hanging)
 * {@link org.geysermc.generator.state.type.sign.SignRotationMapper} (ground_sign_direction)
 *
 * Wall_hanging signs are covered by
 * {@link org.geysermc.generator.state.type.sign.HangingSignAttachedMapper} (attached_bit)
 * {@link org.geysermc.generator.state.type.sign.WallSignFacingMapper} (facing_direction)
 * {@link org.geysermc.generator.state.type.sign.HangingSignHangingMapper} (hanging)
 * {@link org.geysermc.generator.state.type.sign.WallHangingSignRotationMapper} (ground_sign_direction)
 *
 */
package org.geysermc.generator.state.type.sign;
