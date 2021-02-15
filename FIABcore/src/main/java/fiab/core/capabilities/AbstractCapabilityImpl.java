/**
 */
package fiab.core.capabilities;

import ProcessCore.AbstractCapability;
import ProcessCore.Parameter;
import ProcessCore.ProcessCorePackage;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Abstract Capability</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link ProcessCore.impl.AbstractCapabilityImpl#getCapabilities <em>Capabilities</em>}</li>
 *   <li>{@link ProcessCore.impl.AbstractCapabilityImpl#getDisplayName <em>Display Name</em>}</li>
 *   <li>{@link ProcessCore.impl.AbstractCapabilityImpl#getUri <em>Uri</em>}</li>
 *   <li>{@link ProcessCore.impl.AbstractCapabilityImpl#getID <em>ID</em>}</li>
 *   <li>{@link ProcessCore.impl.AbstractCapabilityImpl#getVariables <em>Variables</em>}</li>
 *   <li>{@link ProcessCore.impl.AbstractCapabilityImpl#getInputs <em>Inputs</em>}</li>
 *   <li>{@link ProcessCore.impl.AbstractCapabilityImpl#getOutputs <em>Outputs</em>}</li>
 * </ul>
 *
 * @generated
 */
public class AbstractCapabilityImpl extends MinimalEObjectImpl.Container implements AbstractCapability {
	/**
	 * The cached value of the '{@link #getCapabilities() <em>Capabilities</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCapabilities()
	 * @generated
	 * @ordered
	 */
	protected EList<AbstractCapability> capabilities;

	/**
	 * The default value of the '{@link #getDisplayName() <em>Display Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDisplayName()
	 * @generated
	 * @ordered
	 */
	protected static final String DISPLAY_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getDisplayName() <em>Display Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDisplayName()
	 * @generated
	 * @ordered
	 */
	protected String displayName = DISPLAY_NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getUri() <em>Uri</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUri()
	 * @generated
	 * @ordered
	 */
	protected static final String URI_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getUri() <em>Uri</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUri()
	 * @generated
	 * @ordered
	 */
	protected String uri = URI_EDEFAULT;

	/**
	 * The default value of the '{@link #getID() <em>ID</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getID()
	 * @generated
	 * @ordered
	 */
	protected static final String ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getID() <em>ID</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getID()
	 * @generated
	 * @ordered
	 */
	protected String id = ID_EDEFAULT;

	/**
	 * The cached value of the '{@link #getVariables() <em>Variables</em>}' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getVariables()
	 * @generated
	 * @ordered
	 */
	protected EList<Parameter> variables;

	/**
	 * The cached value of the '{@link #getInputs() <em>Inputs</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInputs()
	 * @generated
	 * @ordered
	 */
	protected EList<Parameter> inputs;

	/**
	 * The cached value of the '{@link #getOutputs() <em>Outputs</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOutputs()
	 * @generated
	 * @ordered
	 */
	protected EList<Parameter> outputs;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected AbstractCapabilityImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return ProcessCorePackage.Literals.ABSTRACT_CAPABILITY;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<AbstractCapability> getCapabilities() {
		if (capabilities == null) {
			capabilities = new EObjectContainmentEList<AbstractCapability>(AbstractCapability.class, this, ProcessCorePackage.ABSTRACT_CAPABILITY__CAPABILITIES);
		}
		return capabilities;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setDisplayName(String newDisplayName) {
		String oldDisplayName = displayName;
		displayName = newDisplayName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ProcessCorePackage.ABSTRACT_CAPABILITY__DISPLAY_NAME, oldDisplayName, displayName));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getUri() {
		return uri;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setUri(String newUri) {
		String oldUri = uri;
		uri = newUri;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ProcessCorePackage.ABSTRACT_CAPABILITY__URI, oldUri, uri));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getID() {
		return id;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setID(String newID) {
		String oldID = id;
		id = newID;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, ProcessCorePackage.ABSTRACT_CAPABILITY__ID, oldID, id));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Parameter> getVariables() {
		if (variables == null) {
			variables = new EObjectResolvingEList<Parameter>(Parameter.class, this, ProcessCorePackage.ABSTRACT_CAPABILITY__VARIABLES);
		}
		return variables;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Parameter> getInputs() {
		if (inputs == null) {
			inputs = new EObjectContainmentEList<Parameter>(Parameter.class, this, ProcessCorePackage.ABSTRACT_CAPABILITY__INPUTS);
		}
		return inputs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<Parameter> getOutputs() {
		if (outputs == null) {
			outputs = new EObjectContainmentEList<Parameter>(Parameter.class, this, ProcessCorePackage.ABSTRACT_CAPABILITY__OUTPUTS);
		}
		return outputs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case ProcessCorePackage.ABSTRACT_CAPABILITY__CAPABILITIES:
				return ((InternalEList<?>)getCapabilities()).basicRemove(otherEnd, msgs);
			case ProcessCorePackage.ABSTRACT_CAPABILITY__INPUTS:
				return ((InternalEList<?>)getInputs()).basicRemove(otherEnd, msgs);
			case ProcessCorePackage.ABSTRACT_CAPABILITY__OUTPUTS:
				return ((InternalEList<?>)getOutputs()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case ProcessCorePackage.ABSTRACT_CAPABILITY__CAPABILITIES:
				return getCapabilities();
			case ProcessCorePackage.ABSTRACT_CAPABILITY__DISPLAY_NAME:
				return getDisplayName();
			case ProcessCorePackage.ABSTRACT_CAPABILITY__URI:
				return getUri();
			case ProcessCorePackage.ABSTRACT_CAPABILITY__ID:
				return getID();
			case ProcessCorePackage.ABSTRACT_CAPABILITY__VARIABLES:
				return getVariables();
			case ProcessCorePackage.ABSTRACT_CAPABILITY__INPUTS:
				return getInputs();
			case ProcessCorePackage.ABSTRACT_CAPABILITY__OUTPUTS:
				return getOutputs();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case ProcessCorePackage.ABSTRACT_CAPABILITY__CAPABILITIES:
				getCapabilities().clear();
				getCapabilities().addAll((Collection<? extends AbstractCapability>)newValue);
				return;
			case ProcessCorePackage.ABSTRACT_CAPABILITY__DISPLAY_NAME:
				setDisplayName((String)newValue);
				return;
			case ProcessCorePackage.ABSTRACT_CAPABILITY__URI:
				setUri((String)newValue);
				return;
			case ProcessCorePackage.ABSTRACT_CAPABILITY__ID:
				setID((String)newValue);
				return;
			case ProcessCorePackage.ABSTRACT_CAPABILITY__VARIABLES:
				getVariables().clear();
				getVariables().addAll((Collection<? extends Parameter>)newValue);
				return;
			case ProcessCorePackage.ABSTRACT_CAPABILITY__INPUTS:
				getInputs().clear();
				getInputs().addAll((Collection<? extends Parameter>)newValue);
				return;
			case ProcessCorePackage.ABSTRACT_CAPABILITY__OUTPUTS:
				getOutputs().clear();
				getOutputs().addAll((Collection<? extends Parameter>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case ProcessCorePackage.ABSTRACT_CAPABILITY__CAPABILITIES:
				getCapabilities().clear();
				return;
			case ProcessCorePackage.ABSTRACT_CAPABILITY__DISPLAY_NAME:
				setDisplayName(DISPLAY_NAME_EDEFAULT);
				return;
			case ProcessCorePackage.ABSTRACT_CAPABILITY__URI:
				setUri(URI_EDEFAULT);
				return;
			case ProcessCorePackage.ABSTRACT_CAPABILITY__ID:
				setID(ID_EDEFAULT);
				return;
			case ProcessCorePackage.ABSTRACT_CAPABILITY__VARIABLES:
				getVariables().clear();
				return;
			case ProcessCorePackage.ABSTRACT_CAPABILITY__INPUTS:
				getInputs().clear();
				return;
			case ProcessCorePackage.ABSTRACT_CAPABILITY__OUTPUTS:
				getOutputs().clear();
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case ProcessCorePackage.ABSTRACT_CAPABILITY__CAPABILITIES:
				return capabilities != null && !capabilities.isEmpty();
			case ProcessCorePackage.ABSTRACT_CAPABILITY__DISPLAY_NAME:
				return DISPLAY_NAME_EDEFAULT == null ? displayName != null : !DISPLAY_NAME_EDEFAULT.equals(displayName);
			case ProcessCorePackage.ABSTRACT_CAPABILITY__URI:
				return URI_EDEFAULT == null ? uri != null : !URI_EDEFAULT.equals(uri);
			case ProcessCorePackage.ABSTRACT_CAPABILITY__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case ProcessCorePackage.ABSTRACT_CAPABILITY__VARIABLES:
				return variables != null && !variables.isEmpty();
			case ProcessCorePackage.ABSTRACT_CAPABILITY__INPUTS:
				return inputs != null && !inputs.isEmpty();
			case ProcessCorePackage.ABSTRACT_CAPABILITY__OUTPUTS:
				return outputs != null && !outputs.isEmpty();
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuffer result = new StringBuffer(super.toString());
		result.append(" (DisplayName: ");
		result.append(displayName);
		result.append(", uri: ");
		result.append(uri);
		result.append(", ID: ");
		result.append(id);
		result.append(')');
		return result.toString();
	}

} //AbstractCapabilityImpl
