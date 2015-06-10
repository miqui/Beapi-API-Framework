package net.nosegrind.apiframework

import org.apache.commons.lang.builder.HashCodeBuilder

class PersonAuthority implements Serializable {

    private static final long serialVersionUID = 1

    Person user
    Authority authority

    boolean equals(other) {
        if (!(other instanceof PersonAuthority)) {
            return false
        }

        other.user?.id == user?.id &&
            other.authority?.id == authority?.id
    }

    int hashCode() {
        def builder = new HashCodeBuilder()
        if (user) builder.append(user.id)
        if (authority) builder.append(authority.id)
        builder.toHashCode()
    }

    static PersonAuthority get(long userId, long authorityId) {
        PersonAuthority.where {
            user == Person.load(userId) &&
            authority == Authority.load(authorityId)
        }.get()
    }

    static boolean exists(long userId, long authorityId) {
        PersonAuthority.where {
            user == Person.load(userId) &&
            authority == Authority.load(authorityId)
        }.count() > 0
    }

    static PersonAuthority create(Person user, Authority authority, boolean flush = false) {
        def instance = new PersonAuthority(user: user, authority: authority)
        instance.save(flush: flush, insert: true)
        instance
    }

    static boolean remove(Person u, Authority r) {
        if (u == null || r == null) return false

        int rowCount = PersonAuthority.where {
            user == Person.load(u.id) &&
                    authority == Authority.load(r.id)
        }.deleteAll()

        rowCount > 0
    }

    static void removeAll(Person u) {
        if (u == null) return

        PersonAuthority.where {
            user == Person.load(u.id)
        }.deleteAll()
    }

    static void removeAll(Authority r) {
        if (r == null) return

        PersonAuthority.where {
            authority == Authority.load(r.id)
        }.deleteAll()
    }

    static constraints = {
        authority validator: { Authority r, PersonAuthority ur ->
            if (ur.user == null) return
            boolean existing = false
            PersonAuthority.withNewSession {
                existing = PersonAuthority.exists(ur.user.id, r.id)
            }
            if (existing) {
                return 'personAuthority.exists'
            }
        }
    }

    static mapping = {
        id composite: ['authority', 'user']
        version false
    }
}
