@ECHO OFF
javadoc -locale en_US -public -notree -nonavbar -noindex -encoding UTF-8 -d ./build/outputs/doc/ -doctitle "<b>VFService Doc</b>" -sourcepath ./build/generated/aidl_source_output_dir/VersionGernal_Release/out/;./src/main/aidl  @VFServicePackages.txt
pause