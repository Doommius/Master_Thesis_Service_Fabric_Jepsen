// ------------------------------------------------------------
//  Copyright (c) Microsoft Corporation.  All rights reserved.
//  Licensed under the MIT License (MIT). See License.txt in the repo root for license information.
// ------------------------------------------------------------

namespace ReliableCollectionsWebAPI.Controllers
{
    using System.Collections.Generic;
    using System.Threading;
    using System.Threading.Tasks;
    using Microsoft.AspNetCore.Mvc;
    using Microsoft.ServiceFabric.Data;
    using Microsoft.ServiceFabric.Data.Collections;
    using System.Net.Http;
    using System;
    using System.Fabric;

    [Route("api/[controller]")]
    public class ReliableQueueController : Controller
    {
        private readonly IReliableStateManager StateManager;

        public ReliableQueueController(IReliableStateManager stateManager)
        {
            this.StateManager = stateManager;
        }

        // get api/ReliableQueue/ to get Queue Length
        [HttpGet("")]
        public async Task<IActionResult> Get()
        {
            IReliableQueue<long> queue = await this.StateManager.GetOrAddAsync<IReliableQueue<long>>("myReliableQueue");
            long returnvalue;
            using (var txn = this.StateManager.CreateTransaction())
            {
                returnvalue = await queue.GetCountAsync(txn);

                
            }
            List<KeyValuePair<string, long>> result = new List<KeyValuePair<string, long>>();
            result.Add(new KeyValuePair<string, long>("Value", returnvalue));
            return this.Json(result);

        }


        // get api/ReliableQueue/ to get Queue Length
        [HttpGet("peek")]
        public async Task<IActionResult> Get(string key)
        {
            IReliableQueue<long> queue = await this.StateManager.GetOrAddAsync<IReliableQueue<long>>("myReliableQueue");

            ConditionalValue<long> returnvalue;


            using (var txn = this.StateManager.CreateTransaction())
            {
                returnvalue = await queue.TryPeekAsync(txn);
 
            }
            if (returnvalue.HasValue)
            {

                List<KeyValuePair<string, long>> result = new List<KeyValuePair<string, long>>();
                result.Add(new KeyValuePair<string, long>("Value", returnvalue.Value));
                return this.Json(result);
            }
            else
            {
                return new OkResult();
            }
        }

        // delete api/ReliableQueue/ to pop element
        [HttpDelete("")]
        public async Task<IActionResult> get()
        {
           
         IReliableQueue<long> queue = await this.StateManager.GetOrAddAsync<IReliableQueue<long>>("myReliableQueue");
            ConditionalValue<long> returnvalue;
            using (var txn = this.StateManager.CreateTransaction())
            {
                returnvalue  = await queue.TryDequeueAsync(txn);

                await txn.CommitAsync();



            }
            if (returnvalue.HasValue)
            {


                List<KeyValuePair<string, long>> result = new List<KeyValuePair<string, long>>();
                result.Add(new KeyValuePair<string, long>("Value", returnvalue.Value));
                return this.Json(result);
            }
            else
            {
                return new OkResult();
            }


        }

        // put api/ReliableQueue/{item} to get Queue item
        [HttpPut("{value}")]
        public async Task<IActionResult> put(int value)
        {
            

            IReliableQueue<int> queue = await this.StateManager.GetOrAddAsync<IReliableQueue<int>>("myReliableQueue");

            using (var txn = this.StateManager.CreateTransaction())
            {
                await queue.EnqueueAsync(txn, value);
                await txn.CommitAsync();
            
                
            }

            List<KeyValuePair<string, int>> result = new List<KeyValuePair<string, int>>();
            result.Add(new KeyValuePair<string, int>("Value", value));
            return this.Json(result);



        }

    }
}