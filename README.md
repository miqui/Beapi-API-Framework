
![alt tag](https://cloud.githubusercontent.com/assets/274764/13708177/095061a0-e762-11e5-8c96-34bdda673fff.png)
# Boomstick(tm) Api Framework

Fully reactive api framework providing automation and simplification of api's for scale. Some features include:

- **automated batching:** all endpoints all batchable by default with AUTH ROLES assignable torestrict. Batching can also be TOGGLED to turn this feature ON/OFF per endpoint.

- **api abstraction of communication logic:** communication logic is abstraction from business logic (or 'resource manager') to allow for easier automation, better communication, CPU bound processing and sharing of IO flow with processes/application in a distributed architecture

- **reloadable IO state:** the data associated with functionality for REQUEST/RESPONSE (usually through annotations) has been removed and abstracted out to a single file per endpoint grouping. This allows for ON-THE-FLY reloading of the state and endpoint security. This also allows for easy update and synchronization will all services/processes that may share in the IO flow and need to synchronize this data (rather than duplicate).

- **api chaining:** rather than using HATEOASto make a request, get a link, make a request, get a link, make a request, etc... api chaining allows for creation of an 'api monad' wherein the output from a related set of apis can be chained allowing the output from one api to be accepted as the input to the next and so on and be passed with ONE REQUEST AND ONE RESPONSE.

- **(2.0) Localized API Cache:** returned resources are cached,stored and updated with requesting ROLE/AUTH. Domains extend a base class that auto update this cache upon create/update/delete. This speeds up your api REQUEST/RESPONSE x10

- **(2.0) API Metrics Reporting:** Call your API's normally but have them deliver a metrics report of time it takes for every class/method to deliver so you can optimize queries, methods services

**FAQ**

**Q: How hard is this to implement?**
**A:** Boomstick is 'Plug-N-Play'. Merely install the plugin and it takes care of the 'REST'. The only thing you have to do is build an IO state file for each controller. This enables us to separate all IO data from functionality so it can be shared with other services in the architecture.

**Q: How do I implement the listener for IO state webhook on my proxy/Message queue?**
**A:** It merely requires an endpoint to send the data to. As a side project, I may actually supply a simple daemon in the future with ehCache to do this for people.
