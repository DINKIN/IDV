

file delete -force thredds
file mkdir thredds
cd thredds
puts "Unjarring thredds.war"
exec jar -xvf ../thredds.war
cd WEB-INF/classes
foreach jar [glob ../lib/*.jar] {
    puts "Unjarring $jar"
    exec jar -xvf $jar
}

puts "Making repositorytds.jar"
puts "pwd: [pwd]"
puts "exec: [exec pwd]"
set files ""
foreach file [glob *] {
    append files " "
    append files "\{[file tail $file]\}"
}

set execLine "jar -cvf ../../../repositorytds.jar $files"
eval exec $execLine


