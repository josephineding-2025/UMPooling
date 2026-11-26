"""Run only in an isolated disposable PostgreSQL/PostGIS Docker container.

Usage: python3 supabase/tests/run_local.py
Never supplies or connects to a Supabase project URL. Requires a running container
named umpooling-sprint2-test initialized with POSTGRES_HOST_AUTH_METHOD=trust and
--network none. Host Auth/Vault/network behaviour is deliberately stubbed.
"""
from pathlib import Path
import os,subprocess
ROOT=Path(__file__).resolve().parents[2]
ENV={k:v for k,v in os.environ.items() if k not in ('DOCKER_HOST','DOCKER_CONTEXT','DOCKER_TLS','DOCKER_TLS_VERIFY','DOCKER_CERT_PATH')}
DOCKER=['docker','--host=unix:///var/run/docker.sock']
PSQL=DOCKER+['exec','-i','umpooling-sprint2-test','psql','-U','postgres','-d','postgres','-v','ON_ERROR_STOP=1','-X']
def sql(text,expected=None):
    result=subprocess.run(PSQL,input=text,text=True,capture_output=True,env=ENV)
    if expected:
        assert result.returncode and expected in result.stderr,(result.stdout,result.stderr)
    else:
        assert result.returncode==0,(result.stdout,result.stderr)
    return result
if __name__=='__main__':
    sql((ROOT/'supabase/tests/fixtures/base_schema.sql').read_text())
    for path in sorted((ROOT/'supabase/migrations').glob('*.sql')):
        if path.name.startswith('20261126000010'):
            sql("""insert into auth.users(id,email) values('00000000-0000-4000-8000-000000000001','legacy-balance@siswa.um.edu.my');
                update public.users set "usersRole"='drivers' where "usersId"='00000000-0000-4000-8000-000000000001';
                insert into public.user_impact_stats(user_id,total_trips,total_distance_km,total_co2_saved_kg)
                values('00000000-0000-4000-8000-000000000001',3,10,2);""")
        sql(path.read_text());print('APPLIED:',path.name,flush=True)
    sql((ROOT/'supabase/checks/01_trip_lifecycle_test.sql').read_text(),expected='TRIP LIFECYCLE TESTS PASSED (rolled back)')
    print('PASS: existing lifecycle regression suite',flush=True)
    for path in sorted((ROOT/'supabase/tests').glob('*_test.sql')):
        sql(path.read_text());print('PASS:',path.name,flush=True)

    from concurrency import run
    run()
