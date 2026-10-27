Local debugging
===============

To locally debug you need to have the following services running:

* A dataverse instance. Internal DANS developers can use the vagrant boxes with development versions of the Data Stations for this. You will need to configure
  access to the admin interface to use the unblock-key. For local development only, run the following inside the Vagrant box from the Payara
  `bin` directory (for example `/usr/local/payara7/bin`), authenticating as admin. Keep
  `dataverse.api.blocked.endpoints=api/admin,api/builtin-users` configured. Replace `s3kretKey` with a development key and configure the calling
  client to use the same key (for direct API calls, send the `X-Dataverse-unblock-key` header):

        ./asadmin delete-jvm-options '-Ddataverse.api.blocked.policy=localhost-only'
        ./asadmin create-jvm-options '-Ddataverse.api.blocked.key=s3kretKey'
        ./asadmin create-jvm-options '-Ddataverse.api.blocked.policy=unblock-key'
        sudo systemctl restart payara

        # When done debugging, restore localhost-only access and remove the development key:
        ./asadmin delete-jvm-options '-Ddataverse.api.blocked.policy=unblock-key'
        ./asadmin create-jvm-options '-Ddataverse.api.blocked.policy=localhost-only'
        ./asadmin delete-jvm-options '-Ddataverse.api.blocked.key=s3kretKey'
        sudo systemctl restart payara

* [dd-validate-dans-bag]{:target="_blank"}. Note that its `validation.baseFolder` configuration property should point to the deposit area or an ancestor of it.

Calling `dd-dataverse-ingest` is most conveniently done through the [dd-dataverse-ingest-cli]{:target=_blank} command line tool.



[dd-validate-dans-bag]: {{ validate_dans_bag_url }}
[dd-dataverse-ingest-cli]: {{ dataverse_ingest_cli_url }}