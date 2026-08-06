import os
root=r'c:\Users\ADMIN\OneDrive\Desktop\first_clone_MindFull_ai\backend\src\main\java\com\mindbridge\behavior\feature\job'
for dp,_,fs in os.walk(root):
 for fn in fs:
  if not fn.endswith('.java'):continue
  p=os.path.join(dp,fn)
  b=open(p,'rb').read()
  if len(b) and b[0]==0xFF and b[1]==0xFE:
   t=b[2:].decode('utf-16-le')
   open(p,'w',encoding='utf-8',newline='').write(t)
   print('CONV',p)
   c+=
  else:
   print('OK',p)
print('Total',c)
