


iquery -aq "remove(foo)"
iquery -aq "remove(target)"
iquery -aq "create array foo <val:int64> [x=1:10,10,0, y=1:10,10,0]"
iquery -aq "store(build_sparse(foo, 42, x=3 and y =4), foo)"

iquery -aq "create array target <val:int64> [newX=1:10,10,0, newY=1:10,10,0]"


iquery -aq "
redimension_store(
 apply(
  cross(
   foo,
   build( <newdim:int64> [dX=-1:1,3,0,dY=-1:1,3,0], 1)
  ),
  newX, x+dX, newY, y+dY
 ),
 target
)

"

