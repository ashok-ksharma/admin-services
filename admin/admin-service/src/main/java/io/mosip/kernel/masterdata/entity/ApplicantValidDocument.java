package io.mosip.kernel.masterdata.entity;

import java.io.Serializable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.mosip.kernel.masterdata.entity.id.ApplicantValidDocumentId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 
 * Entity for Applicant valid document
 * 
 * @author Bal Vikash Sharma
 * @since 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "applicant_valid_document", schema = "master")
public class ApplicantValidDocument extends BaseEntity implements Serializable {

	private static final long serialVersionUID = -5585825705521742941L;

	/**
	 * Field for individual type code
	 */
	@EmbeddedId
	@AttributeOverrides({ @AttributeOverride(name = "appTypeCode", column = @Column(name = "apptyp_code")),
			@AttributeOverride(name = "docCategoryCode", column = @Column(name = "doccat_code")),
			@AttributeOverride(name = "docTypeCode", column = @Column(name = "doctyp_code")) })
	private ApplicantValidDocumentId applicantValidDocumentId;

	@Column(name = "lang_code", nullable = false, length = 3)
	private String langCode;

	/*
	 * ---------------------------------------------------------------------------------
	 * CSV-binding compatibility for admin-service's bulk upload.
	 *
	 * admin's bulk upload binds CSV headers straight onto the entity via Spring Batch's
	 * BeanWrapperFieldSetMapper, which resolves by bean-property name. Its now-deleted
	 * duplicate of this entity exposed appTypeCode / docCategoryCode / docTypeCode as flat
	 * properties, so those are the header names in every existing
	 * applicant_valid_document CSV. These accessors keep them binding through the
	 * @EmbeddedId, so no external file format changes.
	 *
	 * They are invisible to JPA: @EmbeddedId sits on the field, so Hibernate uses field
	 * access for this entity and never consults getters. They are equally invisible on the
	 * wire - ApplicantValidDocumentController returns ApplicantValidDocumentDto, and this
	 * entity is never serialized.
	 *
	 * Removing them changes the bulk-upload CSV contract.
	 * ---------------------------------------------------------------------------------
	 */

	public String getAppTypeCode() {
		return applicantValidDocumentId == null ? null : applicantValidDocumentId.getAppTypeCode();
	}

	public void setAppTypeCode(String appTypeCode) {
		documentId().setAppTypeCode(appTypeCode);
	}

	public String getDocCategoryCode() {
		return applicantValidDocumentId == null ? null : applicantValidDocumentId.getDocCategoryCode();
	}

	public void setDocCategoryCode(String docCategoryCode) {
		documentId().setDocCategoryCode(docCategoryCode);
	}

	public String getDocTypeCode() {
		return applicantValidDocumentId == null ? null : applicantValidDocumentId.getDocTypeCode();
	}

	public void setDocTypeCode(String docTypeCode) {
		documentId().setDocTypeCode(docTypeCode);
	}

	private ApplicantValidDocumentId documentId() {
		if (applicantValidDocumentId == null) {
			applicantValidDocumentId = new ApplicantValidDocumentId();
		}
		return applicantValidDocumentId;
	}
}
