rootProject.name = "articioc-core"

include("articioc-base")

include("examples:swapi")

include("providers:articioc-provider-kafka")
include("providers:articioc-provider-poller")
include("providers:articioc-provider-poller-jdbc")
include("providers:articioc-provider-poller-jdbc-postgres")
include("providers:articioc-provider-poller-jdbc-mariadb")
