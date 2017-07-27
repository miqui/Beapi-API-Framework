package net.nosegrind.apiframework

// Status stat1 = Status.findByStatName('PENDING')?:new Status(statName:"PENDING").save(failOnError:true)
// Status stat2 = Status.findByStatName('DISABLED')?:new Status(statName:"DISABLED").save(failOnError:true)
// Status stat3 = Status.findByStatName('CLOSED')?:new Status(statName:"CLOSED").save(failOnError:true)
// Status stat4 = Status.findByStatName('HIDDEN')?:new Status(statName:"HIDDEN").save(failOnError:true)

class Status {

	String statName

	static constraints = {
		statName size:2..255,nullable:false,blank: false, unique: true
	}
}
