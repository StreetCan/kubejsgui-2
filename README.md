**Full disclaimer**:
I did use AI to do most of the heavy lifting, I understand a lot of people's issue with AI completely in regards to generative slop. Just keep in mind, I am an Engineering student and not a Computer Science student, so I do not have the skillset that would enable me to autonomously create a project like this, not yet anyway (I use assembly and C). This project started out as a really bad stopgap for making recipes and tags for a modpack for me and my friends, I posted it on reddit and people really seemed to like it, I then iterrated on it with AI as a side project and that is how this project was born. If you view this project as AI slop, I completely understand, but this is a tool and not a gameplay mod, so the average user won't be exposed to any "slop". It serves one single purpose, creating scripts, so after thats done you remove the mod. The scripts and templates themselves are human-written and the mod only enables drag and drop functionality.

# KubeJS-GUI-Overhaul
Overhauled version of the mod, now with a template system, and web GUI

To use it:

Step 1: Click a chest with bedrock

<img width="1920" height="1057" alt="javaw_6uSZpcc1OM" src="https://github.com/user-attachments/assets/8ac12425-ec19-4a13-ab2f-79a2b999c7b4" />

Step 2: Either copy and paste the local host link into a browser or just click "Yes" to open it in the browser.

<img width="1920" height="1057" alt="javaw_NuP8oTkWyi" src="https://github.com/user-attachments/assets/f8405404-dd80-4f06-986e-dad96e0ba7d9" />

Step 3: Take a look around the GUI 

<img width="1878" height="1002" alt="opera_a4bTy7dJIc" src="https://github.com/user-attachments/assets/5cc6c20b-782e-4580-8840-ee0789be1754" />

Step 4: Add any items you may want to use later (this step can be done when ever), they will appear in the "ITEM PALETTE"

<img width="1920" height="1057" alt="javaw_CmbKWxoegn" src="https://github.com/user-attachments/assets/4fc2da6e-9f62-4732-b492-533e674df108" />

<img width="1878" height="1002" alt="opera_iRfIDFx2uL" src="https://github.com/user-attachments/assets/efd91fb0-0afa-43c5-83a5-44dde6ecc2ef" />

Step 5: Either make a script or copy it from where ever like the KubeJS wiki and paste it in the script editor in the Template Builder tab (Shapeless recipe example in this case)

<img width="1878" height="1002" alt="opera_voEYdFYHBM" src="https://github.com/user-attachments/assets/19c55092-bf2e-47b5-b07a-b5af58b1f5f6" />

<img width="1878" height="1002" alt="opera_AkFIrpjSm5" src="https://github.com/user-attachments/assets/1bb6738f-3861-4eb2-a746-543a3860f15b" />

Step 6: Drag the canvas elements onto the canvas, these will serve as inputs to the script. These templates dont have to be recipes, they can be any valid script with parameters you can specify.
Name your script, preview the script in the bottom right and save it to either your instance or a universal folder. Instance will save it in your instance's folder and universal will save it outside of it, so you can access it from other instances as well.

<img width="1878" height="1002" alt="opera_cjMPluV28c" src="https://github.com/user-attachments/assets/a62a7454-f584-4f50-9988-adbbfda9a73d" />

<img width="805" height="253" alt="opera_kR4bQnnRkB" src="https://github.com/user-attachments/assets/dd0ede80-7d10-4b12-b1ef-c8fae7b3ff4e" />

(just delete the example data and put your cursor where they were and then click the element you want to substitute it with, or just type it out manually, {{itemcount}} will return the integer that it contains when its used to generate a script, the {{out}} element will output the mod:itemid string when used to make a recipe)

Step 7: Use it in the "My Templates" tab, in this case the array was the best option for shapeless crafting so we just drag the items from the palette to the array, this will output an array of form: [Prefix][ItemID][Suffiix][Delimiter], it will not add a delimiter after the last entry in the array. You can preview the script that will be generated at the bottom, be sure to name and save it

<img width="1878" height="1002" alt="opera_PPciXwbAPr" src="https://github.com/user-attachments/assets/043aba6f-e53f-4ff4-b107-2885673a0972" />

Step 8: You should get a chat notification that the script was made, the notification will specify if you should restart your game (for startup scripts) or use the /reload command (for server scripts, like recipes). In this case it was /reload, so just run /reload.

<img width="1920" height="1057" alt="javaw_ZS4gC21iin" src="https://github.com/user-attachments/assets/a01cfe18-5607-4dd4-b3dc-aa51edf1a62c" />

<img width="1920" height="1057" alt="javaw_r5zyrFqCEd" src="https://github.com/user-attachments/assets/47714b29-4399-4121-abb6-3f2b00e8e248" />
Should say "Reloaded with no KubeJS errors!"

Step 9: Enjoy the new recipe (or script, depends on what you made)

<img width="1920" height="1057" alt="javaw_nGystlQ0q7" src="https://github.com/user-attachments/assets/00418fba-59ef-4ae0-a153-9b3cb51cff2c" />

Note: You can share and use templates, they will retain the data for the canvas elements and other metadata.
Also keep in mind the public repo is still being tested, so you have to make your own recipes for the time being, but you will be able to upload templates and download them.

