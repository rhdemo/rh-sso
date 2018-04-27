import httplib
import sys

host = sys.argv[1];
URLSuffix = '/auth/realms/master/remote-cache/userStorage/size';
#print 'Request=',"http://" + host + ":8080" + URLSuffix;

conn = httplib.HTTPConnection(host + ":8080")
conn.timeout = 5;
conn.request("GET",URLSuffix)
res = conn.getresponse()

if res.status != 200:
    print 'Status=', res.status
    sys.exit(1)
count = res.read();
print "Count in userStorage remoteCache:",count;