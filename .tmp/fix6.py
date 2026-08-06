import os
root=r'c:\Users\ADMIN\OneDrive\Desktop\first_clone_MindFull_ai\backend\src\main\java\com\mindbridge\behavior\feature\job'
for dp,_,fs in os.walk(root):
 for fn in fs:
  if not fn.endswith('.java'):continue
  p=os.path.join(dp,fn)
  b=open(p,'rb').read()
  if any(x==0 for x in b):
   nb=bytes([x for x in b if x!=0])
   open(p,'wb').write(nb)
   print('FIXED',p)
   cc=cc+1
  else:
   print('OK',p)
print('Total',cc)
