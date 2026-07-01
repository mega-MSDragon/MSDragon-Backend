package com.msdragon.backend.parentprofile.entity

import com.msdragon.backend.auth.entity.User
import com.msdragon.backend.common.entity.BaseTimeEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
	name = "parent_profiles",
	uniqueConstraints = [
		UniqueConstraint(name = "uk_parent_profiles_user", columnNames = ["user_id"]),
	],
)
class ParentProfile(
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	val user: User,

	@Column(name = "status", nullable = false, length = 20)
	var status: ParentProfileStatus = ParentProfileStatus.DRAFT,

	@Column(name = "current_step", nullable = false)
	var currentStep: Int = 1,

	@Column(name = "activity_level", length = 20)
	var activityLevel: ActivityLevel? = null,

	@Column(name = "food_preference", length = 40)
	var foodPreference: FoodPreference? = null,

	@Column(name = "avoid_spicy", nullable = false)
	var avoidSpicy: Boolean = false,

	@Column(name = "needs_mobility_assistance")
	var needsMobilityAssistance: Boolean? = null,

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(
		name = "parent_profile_themes",
		joinColumns = [JoinColumn(name = "parent_profile_id")],
		uniqueConstraints = [
			UniqueConstraint(name = "uk_parent_profile_themes", columnNames = ["parent_profile_id", "theme_code"]),
		],
	)
	@Column(name = "theme_code", nullable = false, length = 40)
	var themeCodes: MutableSet<String> = mutableSetOf(),

	@Column(name = "personality_type", length = 40)
	var personalityType: TravelPersonalityTypeCode? = null,

	@Column(name = "completion_percent", nullable = false)
	var completionPercent: Int = 0,

	@Column(name = "completed_at")
	var completedAt: LocalDateTime? = null,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	var id: Long? = null
		protected set
}
