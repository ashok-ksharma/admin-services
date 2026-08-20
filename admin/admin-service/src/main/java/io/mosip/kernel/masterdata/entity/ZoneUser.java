package io.mosip.kernel.masterdata.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "zone_user", schema = "master")
public class ZoneUser extends BaseEntity implements Serializable {
	/**
	 * Generated serialVersionUID
	 */
	private static final long serialVersionUID = -8194849518681293756L;

	@Column(name = "zone_code", nullable = false, length = 36)
	private String zoneCode;

	@Id
	@Column(name = "usr_id", nullable = false, length = 256)
	private String userId;

	@Column(name = "lang_code", nullable = true, length = 3)
	private String langCode;
	
	/**
	 * Returns the user id in lower case. masterdata's zone/user lookups rely on this
	 * case-insensitive form, so it is retained on the canonical entity.
	 *
	 * <p>
	 * Null-guarded for the merged application: admin's bulk upload constructs a
	 * {@code ZoneUser} from CSV before every field is populated, and the un-guarded form
	 * would throw. Note that admin's own copy path is unaffected either way -
	 * {@code io.mosip.admin.config.MapperUtils} copies by field reflection, never through
	 * getters.
	 * </p>
	 */
	public String getUserId() {
		return this.userId == null ? null : this.userId.toLowerCase();
	}

}
