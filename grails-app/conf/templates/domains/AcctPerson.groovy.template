package net.nosegrind.apiframework

class AcctPerson implements Serializable {

	Account acct
	Person person
	boolean owner=false

	static constraints = {
		acct blank: false
		person blank: false
		owner blank:false, validator: { val, obj ->
			if (val == true) {
				AcctPerson acctPerson = AcctPerson.findByAcctAndOwner(obj.acct, val)
				if (acctPerson) {
					return '[APPLICATION OWNER EXISTS]'
				}
			}
		}
	}

	static mapping = {
		cache true
	}

}
