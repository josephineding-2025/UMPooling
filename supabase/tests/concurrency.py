"""Real independent PostgreSQL sessions: never connects to a remote database."""
import concurrent.futures,json,subprocess,threading,uuid
from run_local import ENV,PSQL,sql

def query(text):
    r=subprocess.run(PSQL+['-q','-A','-t'],input=text,text=True,capture_output=True,env=ENV)
    if r.returncode:raise AssertionError(r.stderr)
    return json.loads(next(line for line in reversed(r.stdout.splitlines()) if line.startswith('{')))

def race(statements):
    barrier=threading.Barrier(len(statements))
    def one(statement):
        barrier.wait()
        return subprocess.run(PSQL+['-q','-A','-t'],input=statement,text=True,capture_output=True,env=ENV)
    with concurrent.futures.ThreadPoolExecutor(max_workers=len(statements)) as executor:return list(executor.map(one,statements))

def actor(user,statement):
    return f"begin;set local role authenticated;select set_config('request.jwt.claim.sub','{user}',true);{statement};commit;"

def run():
    d,p,q=[str(uuid.uuid4()) for _ in range(3)]
    sql('insert into auth.users(id,email) values '+','.join(f"('{u}','concurrency-{u}@siswa.um.edu.my')" for u in [d,p,q])+';')
    sql(f"update public.users set \"usersRole\"='drivers' where \"usersId\"='{d}';")
    def offer(seats):
        return query(f"""insert into public.trip_offers(driver_id,origin_geo,destination_geo,cost_per_seat,available_seats,departure_time,distance_km)
            values('{d}',st_setsrid(st_makepoint(101.65,3.12),4326)::geography,st_setsrid(st_makepoint(101.66,3.13),4326)::geography,
            2,{seats},now()+interval '1 hour',10) returning json_build_object('id',trip_offer_id);""")['id']
    def reserve(o,u):return f"select json_build_object('id',public.book_seat_with_quote('{o}',1,101.65,3.12,0))"
    o=offer(1)
    r=race([actor(u,reserve(o,u)) for u in [p,q]])
    assert sum(x.returncode==0 for x in r)==1,[x.stderr for x in r]
    v=query(f"select json_build_object('seats',available_seats,'bookings',(select count(*) from public.bookings where trip_offer_id='{o}')) from public.trip_offers where trip_offer_id='{o}';")
    assert v=={'seats':0,'bookings':1},v
    print('PASS: two independent passengers racing for final seat',flush=True)
    o=offer(2)
    r=race([actor(p,reserve(o,p))]*2)
    assert all(x.returncode==0 for x in r),[x.stderr for x in r]
    ids=[json.loads(next(line for line in x.stdout.splitlines() if line.startswith('{')))['id'] for x in r]
    assert ids[0]==ids[1],ids
    v=query(f"select json_build_object('seats',available_seats) from public.trip_offers where trip_offer_id='{o}';")
    assert v['seats']==1,v
    r=race([actor(p,f"select json_build_object('n',public.cancel_booking('{ids[0]}'))")]*2)
    assert all(x.returncode==0 for x in r),[x.stderr for x in r]
    v=query(f"select json_build_object('seats',available_seats) from public.trip_offers where trip_offer_id='{o}';")
    assert v['seats']==2,v
    print('PASS: concurrent reservation retries and cancellation restore once',flush=True)
    trips=[]
    for _ in range(3):
        o=offer(1);query(actor(p,reserve(o,p)))
        t=query(actor(d,f"select json_build_object('id',public.start_trip('{o}'))"))['id']
        sql(actor(p,f"select public.confirm_trip_end('{t}')"));trips.append(t)
    r=race([actor(d,f"select public.complete_trip('{t}')") for t in trips])
    assert all(x.returncode==0 for x in r),[x.stderr for x in r]
    v=query(f"select json_build_object('trips',total_trips,'co2',total_co2_saved_kg) from public.user_impact_stats where user_id='{d}';")
    assert v=={'trips':2,'co2':2},v
    print('PASS: concurrent completions cannot exceed per-user daily cap',flush=True)
