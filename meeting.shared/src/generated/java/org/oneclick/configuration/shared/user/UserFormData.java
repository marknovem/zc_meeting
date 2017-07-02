package org.oneclick.configuration.shared.user;

import java.util.Set;

import javax.annotation.Generated;

import org.eclipse.scout.rt.shared.data.form.AbstractFormData;
import org.eclipse.scout.rt.shared.data.form.fields.AbstractValueFieldData;
import org.eclipse.scout.rt.shared.data.form.properties.AbstractPropertyData;

/**
 * <b>NOTE:</b><br>
 * This class is auto generated by the Scout SDK. No manual modifications
 * recommended.
 */
@Generated(value = "org.oneclick.configuration.client.user.UserForm", comments = "This class is auto generated by the Scout SDK. No manual modifications recommended.")
public class UserFormData extends AbstractFormData {

	private static final long serialVersionUID = 1L;

	/**
	 * access method for property Autofilled.
	 */
	public Boolean getAutofilled() {
		return getAutofilledProperty().getValue();
	}

	/**
	 * access method for property Autofilled.
	 */
	public void setAutofilled(Boolean autofilled) {
		getAutofilledProperty().setValue(autofilled);
	}

	public AutofilledProperty getAutofilledProperty() {
		return getPropertyByClass(AutofilledProperty.class);
	}

	public ConfirmPassword getConfirmPassword() {
		return getFieldByClass(ConfirmPassword.class);
	}

	public Email getEmail() {
		return getFieldByClass(Email.class);
	}

	/**
	 * access method for property HashedPassword.
	 */
	public String getHashedPassword() {
		return getHashedPasswordProperty().getValue();
	}

	/**
	 * access method for property HashedPassword.
	 */
	public void setHashedPassword(String hashedPassword) {
		getHashedPasswordProperty().setValue(hashedPassword);
	}

	public HashedPasswordProperty getHashedPasswordProperty() {
		return getPropertyByClass(HashedPasswordProperty.class);
	}

	public Login getLogin() {
		return getFieldByClass(Login.class);
	}

	public Password getPassword() {
		return getFieldByClass(Password.class);
	}

	public RolesBox getRolesBox() {
		return getFieldByClass(RolesBox.class);
	}

	public SendUserInviteEmail getSendUserInviteEmail() {
		return getFieldByClass(SendUserInviteEmail.class);
	}

	public TimeZone getTimeZone() {
		return getFieldByClass(TimeZone.class);
	}

	public UserId getUserId() {
		return getFieldByClass(UserId.class);
	}

	public static class AutofilledProperty extends AbstractPropertyData<Boolean> {

		private static final long serialVersionUID = 1L;
	}

	public static class ConfirmPassword extends AbstractValueFieldData<String> {

		private static final long serialVersionUID = 1L;
	}

	public static class Email extends AbstractValueFieldData<String> {

		private static final long serialVersionUID = 1L;
	}

	public static class HashedPasswordProperty extends AbstractPropertyData<String> {

		private static final long serialVersionUID = 1L;
	}

	public static class Login extends AbstractValueFieldData<String> {

		private static final long serialVersionUID = 1L;
	}

	public static class Password extends AbstractValueFieldData<String> {

		private static final long serialVersionUID = 1L;
	}

	public static class RolesBox extends AbstractValueFieldData<Set<Long>> {

		private static final long serialVersionUID = 1L;
	}

	public static class SendUserInviteEmail extends AbstractValueFieldData<Boolean> {

		private static final long serialVersionUID = 1L;
	}

	public static class TimeZone extends AbstractValueFieldData<String> {

		private static final long serialVersionUID = 1L;
	}

	public static class UserId extends AbstractValueFieldData<Long> {

		private static final long serialVersionUID = 1L;
	}
}
