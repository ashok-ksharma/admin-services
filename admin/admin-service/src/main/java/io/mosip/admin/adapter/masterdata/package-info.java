/**
 * Adapters for in-process calls into kernel-masterdata-service, one class per masterdata
 * service ({@code <Service>Adapter}, e.g. {@link io.mosip.admin.adapter.masterdata.MachineAdapter}).
 *
 * <p>
 * Every call another service of this application makes into masterdata goes through an
 * adapter here, so the dependencies on masterdata are all in one place. An adapter passes the
 * request on to masterdata's service unchanged and converts the result into what the caller
 * needs; neither the caller nor masterdata has to change. Adapters are shared by every caller.
 * </p>
 *
 * <p>
 * Conventions:
 * </p>
 * <ul>
 * <li>Methods are named by purpose (e.g. {@code getMachinesMappedToCenter}), never by
 * caller.</li>
 * <li>Methods return what the caller needs. When the caller can use masterdata's DTO as it
 * is, the method returns it directly; when it cannot, the conversion happens inside the
 * adapter, never in the caller.</li>
 * <li>An adapter may use several masterdata beans of the same area; adapters do not hold
 * business rules - those stay in the caller's services.</li>
 * <li>Masterdata's errors propagate unchanged; each caller keeps its own error handling.</li>
 * </ul>
 */
package io.mosip.admin.adapter.masterdata;
