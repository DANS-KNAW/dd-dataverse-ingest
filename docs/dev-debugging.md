Local debugging
===============

To locally debug you need to have the following services running:

* A dataverse instance. Internal DANS developers can use the vagrant boxes with development versions of the Data Stations for this. You will need to configure
  access to the admin interface to use the unblock-key:

        curl -X PUT -d s3kretKey http://localhost:8080/api/admin/settings/:BlockedApiKey
        curl -X PUT -d unblock-key http://localhost:8080/api/admin/settings/:BlockedApiPolicy

        # When done debugging, you can reset the policy to localhost-only:
        curl -X PUT -d localhost-only \
              http://localhost:8080/api/admin/settings/:BlockedApiPolicy/?unblock-key=s3kretKey

* [dd-validate-dans-bag]{:target="_blank"}. Note that its `validation.baseFolder` configuration property should point to the deposit area or an ancestor of it.

Calling `dd-dataverse-ingest` is most conveniently done through the [dd-dataverse-ingest-cli]{:target=_blank} command line tool.



[dd-validate-dans-bag]: {{ validate_dans_bag_url }}
[dd-dataverse-ingest-cli]: {{ dataverse_ingest_cli_url }}